"""
Custom Exception Handler for DRF
Provides consistent error format and logging
"""
import logging
import traceback
from django.core.exceptions import ValidationError as DjangoValidationError
from django.http import Http404
from rest_framework import status
from rest_framework.exceptions import (
    APIException,
    AuthenticationFailed,
    NotAuthenticated,
    PermissionDenied,
    NotFound,
    MethodNotAllowed,
    Throttled,
    ValidationError,
)
from rest_framework.views import exception_handler as drf_exception_handler
from rest_framework.response import Response

logger = logging.getLogger(__name__)

# Error code mapping
ERROR_CODES = {
    'AuthenticationFailed': 'AUTH_FAILED',
    'NotAuthenticated': 'AUTH_REQUIRED',
    'PermissionDenied': 'PERMISSION_DENIED',
    'NotFound': 'NOT_FOUND',
    'Http404': 'NOT_FOUND',
    'MethodNotAllowed': 'METHOD_NOT_ALLOWED',
    'Throttled': 'RATE_LIMIT_EXCEEDED',
    'ValidationError': 'VALIDATION_ERROR',
    'IntegrityError': 'INTEGRITY_ERROR',
    'ParseError': 'PARSE_ERROR',
    'UnsupportedMediaType': 'UNSUPPORTED_MEDIA_TYPE',
}

# Korean error messages
ERROR_MESSAGES = {
    'AUTH_FAILED': '인증에 실패했습니다.',
    'AUTH_REQUIRED': '인증이 필요합니다.',
    'PERMISSION_DENIED': '권한이 없습니다.',
    'NOT_FOUND': '요청한 리소스를 찾을 수 없습니다.',
    'METHOD_NOT_ALLOWED': '허용되지 않은 HTTP 메서드입니다.',
    'RATE_LIMIT_EXCEEDED': '요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요.',
    'VALIDATION_ERROR': '입력 데이터가 유효하지 않습니다.',
    'INTEGRITY_ERROR': '데이터 무결성 오류가 발생했습니다.',
    'PARSE_ERROR': '요청 데이터를 파싱할 수 없습니다.',
    'UNSUPPORTED_MEDIA_TYPE': '지원하지 않는 미디어 타입입니다.',
    'SERVER_ERROR': '서버 오류가 발생했습니다.',
}


def custom_exception_handler(exc, context):
    """
    Custom exception handler that returns consistent error format

    Response format:
    {
        "error": {
            "code": "ERROR_CODE",
            "message": "한국어 메시지",
            "details": { ... },
            "field_errors": { ... }  # For validation errors
        },
        "status": 400
    }
    """
    # Get request info for logging
    request = context.get('request')
    view = context.get('view')

    # Convert Django ValidationError to DRF ValidationError
    if isinstance(exc, DjangoValidationError):
        exc = ValidationError(detail=exc.messages)

    # Convert Http404 to NotFound
    if isinstance(exc, Http404):
        exc = NotFound(detail=str(exc) if str(exc) else '요청한 리소스를 찾을 수 없습니다.')

    # Call DRF's default exception handler first
    response = drf_exception_handler(exc, context)

    if response is not None:
        # Get error code
        exc_class = exc.__class__.__name__
        error_code = ERROR_CODES.get(exc_class, 'UNKNOWN_ERROR')

        # Get message
        if isinstance(exc, Throttled):
            wait_time = int(exc.wait) if exc.wait else 60
            message = f'요청 한도를 초과했습니다. {wait_time}초 후에 다시 시도해주세요.'
        else:
            message = ERROR_MESSAGES.get(error_code, str(exc))

        # Build error details
        details = {}
        field_errors = {}

        if isinstance(response.data, dict):
            # Extract field-specific errors for validation
            if isinstance(exc, ValidationError):
                for key, value in response.data.items():
                    if key not in ['detail', 'non_field_errors']:
                        field_errors[key] = value if isinstance(value, list) else [value]
                    elif key == 'non_field_errors':
                        details['non_field_errors'] = value
                    else:
                        details[key] = value
            else:
                details = response.data
        elif isinstance(response.data, list):
            details['errors'] = response.data
        else:
            details['detail'] = response.data

        # Build custom response
        custom_response_data = {
            'error': {
                'code': error_code,
                'message': message,
            },
            'status': response.status_code
        }

        # Add details if present
        if details:
            custom_response_data['error']['details'] = details

        # Add field errors if present
        if field_errors:
            custom_response_data['error']['field_errors'] = field_errors

        response.data = custom_response_data

        # Log the error
        log_level = logging.WARNING if response.status_code < 500 else logging.ERROR
        logger.log(
            log_level,
            f"API Error [{error_code}]: {message} | "
            f"Path: {request.path if request else 'unknown'} | "
            f"Method: {request.method if request else 'unknown'} | "
            f"View: {view.__class__.__name__ if view else 'unknown'} | "
            f"Status: {response.status_code}"
        )
    else:
        # Handle unexpected exceptions
        logger.error(
            f"Unhandled Exception: {exc.__class__.__name__}: {str(exc)} | "
            f"Path: {request.path if request else 'unknown'} | "
            f"Traceback: {traceback.format_exc()}"
        )

        # Return a generic error response
        response = Response(
            {
                'error': {
                    'code': 'SERVER_ERROR',
                    'message': ERROR_MESSAGES['SERVER_ERROR'],
                    'details': {
                        'exception': exc.__class__.__name__
                    }
                },
                'status': status.HTTP_500_INTERNAL_SERVER_ERROR
            },
            status=status.HTTP_500_INTERNAL_SERVER_ERROR
        )

    return response


class CustomAPIException(APIException):
    """
    Base class for custom API exceptions

    Usage:
        raise CustomAPIException(
            code='CUSTOM_ERROR',
            message='커스텀 에러 메시지',
            status_code=400
        )
    """
    status_code = status.HTTP_400_BAD_REQUEST
    default_code = 'error'
    default_detail = '오류가 발생했습니다.'

    def __init__(self, code=None, message=None, status_code=None, details=None):
        self.code = code or self.default_code
        self.message = message or self.default_detail
        if status_code:
            self.status_code = status_code
        self.details = details
        super().__init__(detail=self.message)


class SessionNotFoundError(CustomAPIException):
    """Session not found"""
    status_code = status.HTTP_404_NOT_FOUND
    default_code = 'SESSION_NOT_FOUND'
    default_detail = '세션을 찾을 수 없습니다.'


class SessionNotStartedError(CustomAPIException):
    """Session not started"""
    status_code = status.HTTP_400_BAD_REQUEST
    default_code = 'SESSION_NOT_STARTED'
    default_detail = '세션이 시작되지 않았습니다.'


class SessionAlreadyEndedError(CustomAPIException):
    """Session already ended"""
    status_code = status.HTTP_400_BAD_REQUEST
    default_code = 'SESSION_ENDED'
    default_detail = '이미 종료된 세션입니다.'


class InstructorOnlyError(CustomAPIException):
    """Instructor only action"""
    status_code = status.HTTP_403_FORBIDDEN
    default_code = 'INSTRUCTOR_ONLY'
    default_detail = '강사만 수행할 수 있는 작업입니다.'


class DeviceIdRequiredError(CustomAPIException):
    """Device ID required for anonymous access"""
    status_code = status.HTTP_400_BAD_REQUEST
    default_code = 'DEVICE_ID_REQUIRED'
    default_detail = 'device_id가 필요합니다.'


class AlreadyEnrolledError(CustomAPIException):
    """Already enrolled"""
    status_code = status.HTTP_409_CONFLICT
    default_code = 'ALREADY_ENROLLED'
    default_detail = '이미 수강 중인 강의입니다.'


class NotEnrolledError(CustomAPIException):
    """Not enrolled"""
    status_code = status.HTTP_400_BAD_REQUEST
    default_code = 'NOT_ENROLLED'
    default_detail = '수강 중인 강의가 아닙니다.'
