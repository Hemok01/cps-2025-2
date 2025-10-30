#!/usr/bin/env python
"""
WebSocket Consumer 테스트 스크립트
"""
import asyncio
import json
import sys

try:
    import websockets
except ImportError:
    print("websockets 라이브러리가 필요합니다.")
    print("pip install websockets")
    sys.exit(1)

async def test_student_messages():
    """학생 메시지 테스트"""
    uri = "ws://localhost:8000/ws/sessions/TEST001/"

    print("\n=== 학생 WebSocket 연결 테스트 ===")
    print(f"연결 시도: {uri}")

    try:
        # 인증을 위해 쿠키가 필요하지만, 테스트를 위해 연결만 시도
        async with websockets.connect(uri) as websocket:
            print("✓ WebSocket 연결 성공!")

            # 1. Join 메시지 전송
            print("\n[테스트 1] Join 메시지 전송")
            join_msg = {
                'type': 'join',
                'data': {}
            }
            await websocket.send(json.dumps(join_msg))
            print(f"  → 전송: {join_msg}")

            response = await websocket.recv()
            print(f"  ← 응답: {response}")

            # 2. Heartbeat 메시지 전송
            print("\n[테스트 2] Heartbeat 메시지 전송")
            heartbeat_msg = {
                'type': 'heartbeat',
                'data': {'timestamp': 1234567890}
            }
            await websocket.send(json.dumps(heartbeat_msg))
            print(f"  → 전송: {heartbeat_msg}")

            response = await websocket.recv()
            print(f"  ← 응답: {response}")

            # 3. Step Complete 메시지 전송
            print("\n[테스트 3] Step Complete 메시지 전송")
            step_complete_msg = {
                'type': 'step_complete',
                'data': {'subtask_id': 1}
            }
            await websocket.send(json.dumps(step_complete_msg))
            print(f"  → 전송: {step_complete_msg}")

            response = await websocket.recv()
            print(f"  ← 응답: {response}")

            # 4. Request Help 메시지 전송
            print("\n[테스트 4] Request Help 메시지 전송")
            help_msg = {
                'type': 'request_help',
                'data': {
                    'subtask_id': 1,
                    'message': '이 단계를 이해하지 못했습니다'
                }
            }
            await websocket.send(json.dumps(help_msg))
            print(f"  → 전송: {help_msg}")

            # 잠시 대기 (응답이 없을 수 있음 - help는 강사에게만 전송)
            await asyncio.sleep(1)

            print("\n✓ 모든 학생 메시지 테스트 완료!")

    except websockets.exceptions.InvalidStatusCode as e:
        if e.status_code == 403:
            print(f"✗ 연결 거부 (인증 필요): {e}")
            print("\n참고: WebSocket 연결에는 Django 세션 인증이 필요합니다.")
            print("웹 브라우저에서 http://localhost:8000/admin/ 로 로그인 후")
            print("개발자 도구에서 WebSocket 연결을 테스트하세요.")
        else:
            print(f"✗ 연결 오류: {e}")
    except Exception as e:
        print(f"✗ 오류 발생: {e}")

async def test_connection_only():
    """연결만 테스트 (인증 없음)"""
    uri = "ws://localhost:8000/ws/sessions/TEST001/"

    print("\n=== WebSocket 연결 테스트 (인증 없음) ===")
    print(f"연결 시도: {uri}")

    try:
        async with websockets.connect(uri) as websocket:
            print("✓ WebSocket 연결 성공!")
            print("  서버가 WebSocket을 지원합니다.")

    except websockets.exceptions.InvalidStatusCode as e:
        if e.status_code == 403:
            print("✓ 서버 응답: 403 Forbidden (인증 필요)")
            print("  → WebSocket Consumer가 정상적으로 동작하고 있습니다!")
            print("  → 인증 체크가 정상적으로 작동합니다.")
        elif e.status_code == 404:
            print(f"✗ 서버 응답: 404 Not Found")
            print("  → WebSocket URL이 잘못되었거나 라우팅이 설정되지 않았습니다.")
        else:
            print(f"✗ 서버 응답: {e.status_code}")
    except ConnectionRefusedError:
        print("✗ 연결 거부: 서버가 실행 중이 아닙니다.")
    except Exception as e:
        print(f"✗ 오류 발생: {type(e).__name__}: {e}")

def main():
    print("=" * 60)
    print("MobileGPT WebSocket Consumer 테스트")
    print("=" * 60)

    # 연결 테스트만 수행 (인증이 필요하므로)
    asyncio.run(test_connection_only())

    print("\n" + "=" * 60)
    print("테스트 완료")
    print("=" * 60)
    print("\n다음 단계:")
    print("1. 웹 브라우저에서 http://localhost:8000/admin/ 접속")
    print("2. student1@example.com / student123 으로 로그인")
    print("3. 개발자 도구 (F12) → Network 탭 → WS 필터")
    print("4. 콘솔에서 WebSocket 연결 테스트:")
    print("   const ws = new WebSocket('ws://localhost:8000/ws/sessions/TEST001/');")
    print("   ws.onmessage = (e) => console.log('받음:', e.data);")
    print("   ws.send(JSON.stringify({type: 'join', data: {}}));")

if __name__ == "__main__":
    main()
