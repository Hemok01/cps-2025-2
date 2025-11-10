"""
Kafka Consumer Management Command
Processes activity logs and saves them to database
"""
import json
import time
from django.core.management.base import BaseCommand
from django.conf import settings
from kafka import KafkaConsumer
from kafka.errors import KafkaError

from apps.logs.models import ActivityLog


class Command(BaseCommand):
    help = 'Run Kafka consumer to process activity logs'

    def add_arguments(self, parser):
        parser.add_argument(
            '--topic',
            type=str,
            default='activity-logs',
            help='Kafka topic to consume from'
        )
        parser.add_argument(
            '--group',
            type=str,
            default='mobilegpt-consumer-group',
            help='Consumer group ID'
        )
        parser.add_argument(
            '--bootstrap-servers',
            type=str,
            default='kafka:9092',
            help='Kafka bootstrap servers'
        )

    def handle(self, *args, **options):
        topic = options['topic']
        group_id = options['group']
        bootstrap_servers = options['bootstrap_servers']

        self.stdout.write(self.style.SUCCESS(
            f'Starting Kafka consumer...\n'
            f'Topic: {topic}\n'
            f'Group: {group_id}\n'
            f'Bootstrap servers: {bootstrap_servers}'
        ))

        # Initialize Kafka consumer
        try:
            consumer = KafkaConsumer(
                topic,
                bootstrap_servers=bootstrap_servers,
                group_id=group_id,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')),
                auto_offset_reset='latest',
                enable_auto_commit=True,
                max_poll_records=10
            )
        except KafkaError as e:
            self.stdout.write(self.style.ERROR(f'Failed to connect to Kafka: {e}'))
            return

        self.stdout.write(self.style.SUCCESS('✓ Connected to Kafka successfully'))

        # Log counter
        log_count = 0

        try:
            for message in consumer:
                try:
                    log_data = message.value
                    self.process_log(log_data)
                    log_count += 1

                    if log_count % 10 == 0:
                        self.stdout.write(self.style.SUCCESS(f'Processed {log_count} logs'))

                except Exception as e:
                    self.stdout.write(self.style.ERROR(f'Error processing message: {e}'))
                    continue

        except KeyboardInterrupt:
            self.stdout.write(self.style.WARNING('\nShutting down consumer...'))
        finally:
            consumer.close()
            self.stdout.write(self.style.SUCCESS(f'Consumer closed. Total logs processed: {log_count}'))

    def process_log(self, log_data):
        """Process a single activity log and save to database"""

        # Save to database
        activity_log = ActivityLog.objects.create(
            session_id=log_data.get('session'),
            subtask_id=log_data.get('subtask'),
            user_id=log_data.get('user_id'),
            event_type=log_data.get('event_type'),
            event_data=log_data.get('event_data', {}),
            screen_info=log_data.get('screen_info', {}),
            node_info=log_data.get('node_info', {}),
            parent_node_info=log_data.get('parent_node_info'),
            view_id_resource_name=log_data.get('view_id_resource_name', ''),
            content_description=log_data.get('content_description', ''),
            is_sensitive_data=log_data.get('is_sensitive_data', False)
        )

        self.stdout.write(self.style.SUCCESS(
            f'✓ Saved ActivityLog {activity_log.id} (event: {log_data.get("event_type")}) '
            f'for user {log_data.get("user_id")}'
        ))
