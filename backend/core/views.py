"""
Core views for MobileGPT backend
"""
import os
from django.http import JsonResponse, FileResponse, Http404
from django.views.decorators.http import require_http_methods
from django.conf import settings


def serve_media_with_cors(request, path):
    """
    Serve media files with CORS headers for cross-origin requests.

    This is needed because Django's static() helper doesn't apply
    CORS middleware to media file responses.
    """
    file_path = os.path.join(settings.MEDIA_ROOT, path)

    if not os.path.exists(file_path):
        raise Http404("File not found")

    # Determine content type based on file extension
    content_type = 'application/octet-stream'
    ext = os.path.splitext(path)[1].lower()
    content_types = {
        '.jpg': 'image/jpeg',
        '.jpeg': 'image/jpeg',
        '.png': 'image/png',
        '.gif': 'image/gif',
        '.webp': 'image/webp',
        '.svg': 'image/svg+xml',
    }
    content_type = content_types.get(ext, content_type)

    response = FileResponse(open(file_path, 'rb'), content_type=content_type)

    # Add CORS headers
    origin = request.headers.get('Origin', '')
    allowed_origins = getattr(settings, 'CORS_ALLOWED_ORIGINS', [])

    if origin in allowed_origins or settings.DEBUG:
        response['Access-Control-Allow-Origin'] = origin if origin else '*'
        response['Access-Control-Allow-Methods'] = 'GET, OPTIONS'
        response['Access-Control-Allow-Headers'] = 'Content-Type, Authorization'

    return response


@require_http_methods(["GET"])
def assetlinks(request):
    """
    Serve Android App Links verification file

    This endpoint is used by Android to verify that the app should handle
    links for this domain. Required for HTTPS App Links to work.

    To enable App Links:
    1. Replace YOUR_PACKAGE_NAME with actual package name
    2. Replace SHA256_CERT_FINGERPRINT with your app's signing certificate fingerprint
    3. Deploy to production domain

    Get certificate fingerprint:
    keytool -list -v -keystore your-keystore.jks -alias your-key-alias
    """
    return JsonResponse([
        {
            "relation": ["delegate_permission/common.handle_all_urls"],
            "target": {
                "namespace": "android_app",
                "package_name": "com.mobilegpt.student",  # TODO: Update if package name changes
                "sha256_cert_fingerprints": [
                    # TODO: Add your production signing certificate SHA-256 fingerprint here
                    # Example: "14:6D:E9:83:C5:73:06:50:D8:EE:B9:95:2F:34:FC:64:16:A0:83:42:E6:1D:BE:A8:8A:04:96:B2:3F:CF:44:E5"
                ]
            }
        }
    ], safe=False)
