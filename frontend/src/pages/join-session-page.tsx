import { useParams } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "../components/ui/card";
import { Button } from "../components/ui/button";
import { Alert, AlertDescription } from "../components/ui/alert";
import { Smartphone, Download, QrCode, CheckCircle } from "lucide-react";

export function JoinSessionPage() {
  const { sessionCode } = useParams<{ sessionCode: string }>();

  const handleDownloadApk = () => {
    // Direct APK download from backend
    const apkUrl = `${import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000'}/static/downloads/mobilegpt-student.apk`;
    window.open(apkUrl, "_blank");
  };

  const handleOpenApp = () => {
    // Try to open the app using deep link
    window.location.href = `mobilegpt://join/${sessionCode}`;

    // Fallback: If app doesn't open in 2 seconds, show install message
    setTimeout(() => {
      const confirmDownload = confirm(
        "앱이 설치되어 있지 않은 것 같습니다. APK 파일을 다운로드하시겠습니까?"
      );
      if (confirmDownload) {
        handleDownloadApk();
      }
    }, 2000);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center p-4">
      <Card className="max-w-2xl w-full">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 w-20 h-20 bg-blue-600 rounded-full flex items-center justify-center">
            <Smartphone className="w-10 h-10 text-white" />
          </div>
          <CardTitle className="text-3xl mb-2">MobileGPT Student</CardTitle>
          <CardDescription className="text-lg">
            스마트폰 학습을 위한 수강생 앱
          </CardDescription>
        </CardHeader>

        <CardContent className="space-y-6">
          {/* Session Code Display */}
          {sessionCode && (
            <Alert className="bg-blue-50 border-blue-200">
              <QrCode className="h-5 w-5 text-blue-600" />
              <AlertDescription className="ml-2">
                <div className="flex items-center justify-between">
                  <div>
                    <p className="font-semibold text-blue-900">수업 코드</p>
                    <p className="text-2xl font-bold tracking-wider text-blue-600 mt-1">
                      {sessionCode}
                    </p>
                  </div>
                </div>
              </AlertDescription>
            </Alert>
          )}

          {/* App Description */}
          <div className="space-y-3">
            <h3 className="font-semibold text-lg">앱 기능</h3>
            <ul className="space-y-2">
              {[
                "실시간 수업 참여 및 학습 활동 기록",
                "스마트폰 사용 행동 분석 및 피드백",
                "학습 진도 추적 및 과제 수행",
                "강사와의 실시간 소통 및 도움 요청",
              ].map((feature, index) => (
                <li key={index} className="flex items-start gap-2">
                  <CheckCircle className="w-5 h-5 text-green-600 flex-shrink-0 mt-0.5" />
                  <span>{feature}</span>
                </li>
              ))}
            </ul>
          </div>

          {/* Action Buttons */}
          <div className="space-y-3">
            <Button
              onClick={handleOpenApp}
              className="w-full gap-2 text-lg h-12"
              style={{ backgroundColor: "var(--primary)" }}
            >
              <Smartphone className="w-5 h-5" />
              앱이 설치되어 있으면 수업 참가
            </Button>

            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <span className="w-full border-t" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white px-2 text-gray-500">또는</span>
              </div>
            </div>

            <Button
              onClick={handleDownloadApk}
              variant="outline"
              className="w-full gap-2 text-lg h-12 border-2"
            >
              <Download className="w-5 h-5" />
              APK 파일 다운로드
            </Button>
          </div>

          {/* Instructions */}
          <Alert>
            <AlertDescription className="text-sm space-y-2">
              <p className="font-semibold">📱 설치 및 참가 방법:</p>
              <ol className="list-decimal list-inside space-y-1 ml-2">
                <li>위 버튼을 눌러 APK 파일을 다운로드하세요</li>
                <li>다운로드한 APK 파일을 열어 설치하세요</li>
                <li>"출처를 알 수 없는 앱" 설치 권한을 허용하세요</li>
                <li>앱을 실행하고 로그인하세요</li>
                <li>위에 표시된 수업 코드를 입력하거나 QR 코드를 다시 스캔하세요</li>
              </ol>
            </AlertDescription>
          </Alert>

          {/* Security Notice */}
          <Alert className="bg-yellow-50 border-yellow-200">
            <AlertDescription className="text-sm">
              <p className="font-semibold text-yellow-800 mb-1">⚠️ 보안 안내</p>
              <p className="text-yellow-700">
                APK 설치 시 "출처를 알 수 없는 앱" 경고가 표시될 수 있습니다.
                이는 정상적인 현상이며, 설정에서 권한을 허용하면 설치할 수 있습니다.
              </p>
            </AlertDescription>
          </Alert>

          {/* Note */}
          <div className="text-center text-sm text-gray-500 pt-4 border-t">
            <p>이 앱은 Android 기기에서만 사용 가능합니다</p>
            <p className="mt-1">문제가 있으시면 강사에게 문의하세요</p>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
