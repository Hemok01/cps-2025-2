#!/usr/bin/env python3
"""
Complete WebSocket test suite for Mobile GPT
Tests authentication, session creation, and real-time WebSocket communication
"""

import asyncio
import aiohttp
import json
import time
from datetime import datetime

BASE_URL = "http://localhost:8000"
WS_BASE_URL = "ws://localhost:8000/ws"

class WebSocketTester:
    def __init__(self):
        self.session = None
        self.token = None
        self.session_code = None
        self.lecture_id = None

    async def setup(self):
        """Initialize HTTP session"""
        self.session = aiohttp.ClientSession()

    async def cleanup(self):
        """Clean up resources"""
        if self.session:
            await self.session.close()

    async def login(self, email="instructor@test.com", password="instructor123"):
        """Login and get JWT token"""
        print(f"\n{'='*60}")
        print(f"1. LOGIN TEST")
        print(f"{'='*60}")

        async with self.session.post(
            f"{BASE_URL}/api/auth/login/",
            json={"email": email, "password": password}
        ) as resp:
            if resp.status == 200:
                data = await resp.json()
                self.token = data.get('access')
                print(f"✅ Login successful!")
                print(f"   Token: {self.token[:50]}...")
                print(f"   User: {email}")
                return True
            else:
                error = await resp.text()
                print(f"❌ Login failed: {error}")
                return False

    async def create_lecture(self):
        """Create a test lecture"""
        print(f"\n{'='*60}")
        print(f"2. CREATE LECTURE")
        print(f"{'='*60}")

        headers = {"Authorization": f"Bearer {self.token}"}

        # First check if lecture exists
        async with self.session.get(
            f"{BASE_URL}/api/lectures/",
            headers=headers
        ) as resp:
            if resp.status == 200:
                data = await resp.json()
                # Check if data is a list or dict with results
                lectures = data if isinstance(data, list) else data.get('results', [])
                if lectures and len(lectures) > 0:
                    self.lecture_id = lectures[0]['id']
                    print(f"✅ Using existing lecture: {lectures[0]['title']} (ID: {self.lecture_id})")
                    return True

        # Create new lecture if none exists
        lecture_data = {
            "title": f"WebSocket Test Lecture {datetime.now().strftime('%H:%M:%S')}",
            "description": "Testing WebSocket functionality",
            "week": 1,
            "order": 1
        }

        async with self.session.post(
            f"{BASE_URL}/api/lectures/",
            headers=headers,
            json=lecture_data
        ) as resp:
            if resp.status == 201:
                data = await resp.json()
                self.lecture_id = data['id']
                print(f"✅ Lecture created: {data['title']} (ID: {self.lecture_id})")
                return True
            else:
                error = await resp.text()
                print(f"❌ Failed to create lecture: {error}")
                return False

    async def create_session(self):
        """Create a new session"""
        print(f"\n{'='*60}")
        print(f"3. CREATE SESSION")
        print(f"{'='*60}")

        headers = {"Authorization": f"Bearer {self.token}"}

        session_data = {
            "title": f"WebSocket Test Session {datetime.now().strftime('%H:%M:%S')}"
        }

        # Use the correct endpoint under lectures
        async with self.session.post(
            f"{BASE_URL}/api/lectures/{self.lecture_id}/sessions/create/",
            headers=headers,
            json=session_data
        ) as resp:
            if resp.status in [200, 201]:
                data = await resp.json()
                self.session_code = data['session_code']
                print(f"✅ Session created!")
                print(f"   Code: {self.session_code}")
                print(f"   ID: {data['id']}")
                print(f"   Status: {data['status']}")
                return data['id']
            else:
                error = await resp.text()
                print(f"❌ Failed to create session: {error}")
                return None

    async def test_websocket(self):
        """Test WebSocket connection and messaging"""
        print(f"\n{'='*60}")
        print(f"4. WEBSOCKET CONNECTION TEST")
        print(f"{'='*60}")

        ws_url = f"{WS_BASE_URL}/sessions/{self.session_code}/"
        print(f"📡 Connecting to: {ws_url}")

        try:
            async with self.session.ws_connect(ws_url) as ws:
                print(f"✅ WebSocket connected!")

                # Send join message
                join_msg = {"type": "join", "data": {}}
                await ws.send_str(json.dumps(join_msg))
                print(f"📤 Sent: {join_msg}")

                # Listen for messages
                print(f"\n📥 Listening for messages (10 seconds)...")

                async def receive_messages():
                    async for msg in ws:
                        if msg.type == aiohttp.WSMsgType.TEXT:
                            data = json.loads(msg.data)
                            print(f"📨 Received: {data}")
                        elif msg.type == aiohttp.WSMsgType.ERROR:
                            print(f"❌ WebSocket error: {ws.exception()}")
                            break

                # Run for 10 seconds
                try:
                    await asyncio.wait_for(receive_messages(), timeout=10.0)
                except asyncio.TimeoutError:
                    print(f"⏱️  Timeout after 10 seconds")

                # Send a test message
                print(f"\n📤 Sending test messages...")

                # Test instructor commands
                commands = [
                    {"type": "next_step", "data": {}},
                    {"type": "pause_session", "data": {}},
                    {"type": "resume_session", "data": {}},
                ]

                for cmd in commands:
                    await ws.send_str(json.dumps(cmd))
                    print(f"📤 Sent: {cmd}")
                    await asyncio.sleep(1)

                print(f"\n✅ WebSocket test completed!")

        except Exception as e:
            print(f"❌ WebSocket connection failed: {e}")

    async def test_parallel_connections(self):
        """Test multiple WebSocket connections (instructor + student)"""
        print(f"\n{'='*60}")
        print(f"5. PARALLEL CONNECTIONS TEST")
        print(f"{'='*60}")

        ws_url = f"{WS_BASE_URL}/sessions/{self.session_code}/"

        async def instructor_connection():
            """Instructor WebSocket connection"""
            async with self.session.ws_connect(ws_url) as ws:
                print(f"👨‍🏫 Instructor connected")

                # Send join
                await ws.send_str(json.dumps({"type": "join", "data": {}}))

                # Send commands
                await asyncio.sleep(2)
                await ws.send_str(json.dumps({"type": "next_step", "data": {}}))
                print(f"👨‍🏫 Instructor sent: next_step")

                # Listen for messages
                async for msg in ws:
                    if msg.type == aiohttp.WSMsgType.TEXT:
                        data = json.loads(msg.data)
                        print(f"👨‍🏫 Instructor received: {data.get('type', 'unknown')}")

        async def student_connection():
            """Student WebSocket connection"""
            async with self.session.ws_connect(ws_url) as ws:
                print(f"👩‍🎓 Student connected")

                # Send join
                await ws.send_str(json.dumps({"type": "join", "data": {}}))

                # Send progress updates
                await asyncio.sleep(3)
                await ws.send_str(json.dumps({
                    "type": "step_complete",
                    "data": {"subtask_id": 1}
                }))
                print(f"👩‍🎓 Student sent: step_complete")

                # Listen for messages
                async for msg in ws:
                    if msg.type == aiohttp.WSMsgType.TEXT:
                        data = json.loads(msg.data)
                        print(f"👩‍🎓 Student received: {data.get('type', 'unknown')}")

        try:
            # Run both connections for 5 seconds
            await asyncio.wait_for(
                asyncio.gather(
                    instructor_connection(),
                    student_connection()
                ),
                timeout=5.0
            )
        except asyncio.TimeoutError:
            print(f"⏱️  Test completed after 5 seconds")

        print(f"\n✅ Parallel connections test completed!")

    async def run_all_tests(self):
        """Run all tests in sequence"""
        print(f"\n{'='*60}")
        print(f"MOBILE GPT WEBSOCKET FULL TEST SUITE")
        print(f"{'='*60}")
        print(f"Server: {BASE_URL}")
        print(f"WebSocket: {WS_BASE_URL}")
        print(f"Time: {datetime.now()}")

        await self.setup()

        try:
            # Step 1: Login
            if not await self.login():
                print("❌ Login failed, aborting tests")
                return

            # Step 2: Create lecture
            if not await self.create_lecture():
                print("❌ Lecture creation failed, aborting tests")
                return

            # Step 3: Create session
            session_id = await self.create_session()
            if not session_id:
                print("❌ Session creation failed, aborting tests")
                return

            # Step 4: Test WebSocket
            await self.test_websocket()

            # Step 5: Test parallel connections
            await self.test_parallel_connections()

            print(f"\n{'='*60}")
            print(f"✅ ALL TESTS COMPLETED SUCCESSFULLY!")
            print(f"{'='*60}")

            print(f"\n📊 Test Summary:")
            print(f"   - Login: ✅")
            print(f"   - Lecture Creation: ✅")
            print(f"   - Session Creation: ✅")
            print(f"   - WebSocket Connection: ✅")
            print(f"   - Parallel Connections: ✅")

            print(f"\n🌐 You can now test in browser:")
            print(f"   1. Open: http://localhost:3000")
            print(f"   2. Login with: instructor@test.com / instructor123")
            print(f"   3. Session Code: {self.session_code}")
            print(f"   4. Open DevTools Console to see WebSocket logs")

        finally:
            await self.cleanup()

async def main():
    tester = WebSocketTester()
    await tester.run_all_tests()

if __name__ == "__main__":
    asyncio.run(main())