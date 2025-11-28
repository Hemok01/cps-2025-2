"""
Kafka Producer for Activity Logs
"""
import json
import logging
from typing import Dict, List, Optional
from kafka import KafkaProducer
from kafka.errors import KafkaError
from django.conf import settings

logger = logging.getLogger(__name__)


class ActivityLogProducer:
    """
    Kafka Producer for sending activity logs
    Uses singleton pattern to reuse producer instance
    """
    _instance = None
    _producer = None

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance

    def __init__(self):
        if self._producer is None:
            self._initialize_producer()

    def _initialize_producer(self):
        """Initialize Kafka Producer with configuration"""
        try:
            self._producer = KafkaProducer(
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS.split(','),
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                acks='all',  # Wait for all replicas to acknowledge
                retries=3,  # Retry up to 3 times
                max_in_flight_requests_per_connection=5,
                compression_type='gzip',  # Compress messages
                linger_ms=10,  # Batch messages for 10ms
                batch_size=16384,  # 16KB batch size
            )
            logger.info("Kafka Producer initialized successfully")
        except Exception as e:
            logger.error(f"Failed to initialize Kafka Producer: {e}")
            self._producer = None

    def _on_send_success(self, record_metadata):
        """Callback for successful message send"""
        logger.debug(
            f"Message sent to {record_metadata.topic} "
            f"partition {record_metadata.partition} "
            f"offset {record_metadata.offset}"
        )

    def _on_send_error(self, exc):
        """Callback for failed message send"""
        logger.error(f"Failed to send message to Kafka: {exc}")

    def send_log(self, log_data: Dict, user_id: Optional[int] = None, device_id: Optional[str] = None) -> bool:
        """
        Send a single activity log to Kafka

        Args:
            log_data: Activity log data dictionary
            user_id: User ID for logging purposes (optional for anonymous users)
            device_id: Device ID for anonymous users (optional)

        Returns:
            bool: True if message was queued successfully, False otherwise
        """
        if self._producer is None:
            logger.warning("Kafka Producer not available, skipping send")
            return False

        try:
            # Add user_id or device_id to log data
            message_data = {**log_data}
            if user_id is not None:
                message_data['user_id'] = user_id
            if device_id is not None:
                message_data['device_id'] = device_id

            # Send message asynchronously with callbacks
            future = self._producer.send(
                settings.KAFKA_TOPICS['ACTIVITY_LOG'],
                value=message_data
            )

            # Add callbacks
            future.add_callback(self._on_send_success)
            future.add_errback(self._on_send_error)

            identifier = f"user {user_id}" if user_id else f"device {device_id}"
            logger.debug(f"Activity log queued for {identifier}")
            return True

        except KafkaError as e:
            identifier = f"user {user_id}" if user_id else f"device {device_id}"
            logger.error(f"Kafka error sending log for {identifier}: {e}")
            return False
        except Exception as e:
            identifier = f"user {user_id}" if user_id else f"device {device_id}"
            logger.error(f"Unexpected error sending log for {identifier}: {e}")
            return False

    def send_logs_batch(self, logs_data: List[Dict], user_id: int) -> bool:
        """
        Send multiple activity logs to Kafka

        Args:
            logs_data: List of activity log data dictionaries
            user_id: User ID for logging purposes

        Returns:
            bool: True if all messages were queued successfully, False otherwise
        """
        if self._producer is None:
            logger.warning("Kafka Producer not available, skipping batch send")
            return False

        try:
            success_count = 0

            for log_data in logs_data:
                # Add user_id to each log
                message_data = {
                    'user_id': user_id,
                    **log_data
                }

                # Send message
                future = self._producer.send(
                    settings.KAFKA_TOPICS['ACTIVITY_LOG'],
                    value=message_data
                )
                future.add_callback(self._on_send_success)
                future.add_errback(self._on_send_error)
                success_count += 1

            # Flush to ensure messages are sent
            self._producer.flush(timeout=10)

            logger.info(f"Batch of {success_count} logs queued for user {user_id}")
            return True

        except KafkaError as e:
            logger.error(f"Kafka error sending batch logs for user {user_id}: {e}")
            return False
        except Exception as e:
            logger.error(f"Unexpected error sending batch logs for user {user_id}: {e}")
            return False

    def close(self):
        """Close Kafka Producer connection"""
        if self._producer:
            try:
                self._producer.flush()
                self._producer.close()
                logger.info("Kafka Producer closed")
            except Exception as e:
                logger.error(f"Error closing Kafka Producer: {e}")
            finally:
                self._producer = None


# Global producer instance
producer = ActivityLogProducer()
