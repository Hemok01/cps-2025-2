"""
Custom Exception Handler for DRF
"""
from rest_framework.views import exception_handler as drf_exception_handler
from rest_framework.response import Response


def custom_exception_handler(exc, context):
    """
    Custom exception handler that returns consistent error format
    """
    response = drf_exception_handler(exc, context)

    if response is not None:
        custom_response_data = {
            'error': {
                'code': exc.__class__.__name__,
                'message': str(exc),
                'details': response.data if isinstance(response.data, dict) else {'detail': response.data}
            }
        }
        response.data = custom_response_data

    return response
