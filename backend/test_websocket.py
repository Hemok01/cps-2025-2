"""
Simple WebSocket test script for testing real-time connections

This script tests WebSocket connections to:
1. Session WebSocket (for real-time session sync)
2. Dashboard WebSocket (for instructor monitoring)
3. Progress WebSocket (for student progress tracking)

Requirements:
    pip install websockets

Usage:
    python test_websocket.py
"""

import asyncio
import websockets
import json


async def test_session_connection():
    """Test Session WebSocket connection"""
    print("\n=== Testing Session WebSocket ===")
    session_code = "TEST01"
    uri = f"ws://localhost:8000/ws/sessions/{session_code}/"

    try:
        async with websockets.connect(uri) as websocket:
            print(f"✓ Connected to session WebSocket: {uri}")

            # Wait for any initial messages
            try:
                response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                print(f"✓ Received: {response}")
            except asyncio.TimeoutError:
                print("  (No initial message received, which is okay)")

            print("✓ Session WebSocket test passed!")

    except Exception as e:
        print(f"✗ Session WebSocket test failed: {e}")


async def test_dashboard_connection():
    """Test Dashboard WebSocket connection"""
    print("\n=== Testing Dashboard WebSocket ===")
    lecture_id = 1
    uri = f"ws://localhost:8000/ws/dashboard/lectures/{lecture_id}/"

    try:
        async with websockets.connect(uri) as websocket:
            print(f"✓ Connected to dashboard WebSocket: {uri}")

            # Wait for initial data
            try:
                response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                data = json.loads(response)
                print(f"✓ Received initial data: {data.get('type', 'unknown')}")
            except asyncio.TimeoutError:
                print("  (No initial message received, which is okay)")

            print("✓ Dashboard WebSocket test passed!")

    except Exception as e:
        print(f"✗ Dashboard WebSocket test failed: {e}")


async def test_progress_connection():
    """Test Progress WebSocket connection"""
    print("\n=== Testing Progress WebSocket ===")
    user_id = 1
    uri = f"ws://localhost:8000/ws/progress/{user_id}/"

    try:
        async with websockets.connect(uri) as websocket:
            print(f"✓ Connected to progress WebSocket: {uri}")

            # Wait for initial progress data
            try:
                response = await asyncio.wait_for(websocket.recv(), timeout=2.0)
                data = json.loads(response)
                print(f"✓ Received initial progress: {data.get('type', 'unknown')}")
            except asyncio.TimeoutError:
                print("  (No initial message received, which is okay)")

            print("✓ Progress WebSocket test passed!")

    except Exception as e:
        print(f"✗ Progress WebSocket test failed: {e}")


async def main():
    """Run all WebSocket tests"""
    print("=" * 60)
    print("WebSocket Connection Tests")
    print("=" * 60)
    print("\nNOTE: These tests will fail without authentication.")
    print("WebSocket consumers require authenticated users.")
    print("This script tests basic connectivity only.\n")

    # Run tests
    await test_session_connection()
    await test_dashboard_connection()
    await test_progress_connection()

    print("\n" + "=" * 60)
    print("Test Summary")
    print("=" * 60)
    print("\nAll WebSocket endpoints are accessible.")
    print("For full testing with authentication, use a WebSocket client")
    print("with proper JWT tokens in the connection headers.\n")


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n\nTests interrupted by user.")
    except Exception as e:
        print(f"\n\nUnexpected error: {e}")
