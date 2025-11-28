"""
Core views for MobileGPT backend
"""
from django.http import JsonResponse
from django.views.decorators.http import require_http_methods


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
