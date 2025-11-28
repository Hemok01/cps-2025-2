#!/usr/bin/env python3
import asyncio
import websockets
import json

async def test():
    uri = "ws://localhost:8001/ws/sessions/P39T7E/"
    print("[WS] Connecting...")
    async with websockets.connect(uri) as ws:
        print("[WS] Connected")
        await ws.send(json.dumps({"type":"join","data":{"device_id":"test-device","name":"Test"}}))
        msg = await ws.recv()
        print(f"[WS] {json.loads(msg).get('type')}")
        print("[WS] Waiting for broadcast...")
        try:
            while True:
                msg = await asyncio.wait_for(ws.recv(), timeout=30)
                data = json.loads(msg)
                print(f"[WS] *** BROADCAST: {data} ***")
        except asyncio.TimeoutError:
            print("[WS] Timeout")

asyncio.run(test())
