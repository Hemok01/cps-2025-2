#!/bin/bash

# Session Control API Test Script
# Tests: Start, Next-Step, Pause, Resume, End

echo "=========================================="
echo "Session Control API Test"
echo "=========================================="
echo ""

# 1. Get instructor token
echo "=== 1. Getting Instructor Token ==="
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8000/api/token/ \
  -H "Content-Type: application/json" \
  -d '{"email": "instructor@test.com", "password": "test1234"}')

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['access'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to get token. Please check if instructor account exists."
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo "✅ Token obtained successfully"
echo ""

# 2. Get or Create Lecture
echo "=== 2. Getting Lecture ==="
LECTURES=$(curl -s -X GET http://localhost:8000/api/lectures/ \
  -H "Authorization: Bearer $TOKEN")

LECTURE_ID=$(echo $LECTURES | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['results'][0]['id'] if data.get('results') else '')" 2>/dev/null)

if [ -z "$LECTURE_ID" ]; then
  echo "❌ No lecture found. Creating one..."
  CREATE_LECTURE=$(curl -s -X POST http://localhost:8000/api/lectures/ \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"title": "Session Control Test Lecture", "description": "Test lecture for session control"}')

  LECTURE_ID=$(echo $CREATE_LECTURE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

  if [ -z "$LECTURE_ID" ]; then
    echo "❌ Failed to create lecture"
    exit 1
  fi
  echo "✅ Lecture created (ID: $LECTURE_ID)"
else
  echo "✅ Using existing lecture (ID: $LECTURE_ID)"
fi
echo ""

# 3. Create Task
echo "=== 3. Creating Task ==="
TASK_RESPONSE=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "테스트 작업", "description": "세션 제어 테스트용 작업", "order_index": 1}')

TASK_ID=$(echo $TASK_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

if [ -z "$TASK_ID" ]; then
  echo "❌ Failed to create task"
  echo "Response: $TASK_RESPONSE"
  exit 1
fi

echo "✅ Task created (ID: $TASK_ID)"
echo ""

# 4. Create Subtasks
echo "=== 4. Creating Subtasks ==="

# Subtask 1
SUBTASK1=$(curl -s -X POST http://localhost:8000/api/tasks/$TASK_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "1단계: 앱 열기", "description": "유튜브 앱을 엽니다", "order_index": 1, "guide_text": "유튜브 앱을 터치하여 엽니다"}')

SUBTASK1_ID=$(echo $SUBTASK1 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

# Subtask 2
SUBTASK2=$(curl -s -X POST http://localhost:8000/api/tasks/$TASK_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "2단계: 검색하기", "description": "검색창에서 원하는 내용을 검색합니다", "order_index": 2, "guide_text": "상단 검색창을 터치하여 검색어를 입력합니다"}')

SUBTASK2_ID=$(echo $SUBTASK2 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

# Subtask 3
SUBTASK3=$(curl -s -X POST http://localhost:8000/api/tasks/$TASK_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "3단계: 동영상 재생", "description": "원하는 동영상을 선택하여 재생합니다", "order_index": 3, "guide_text": "원하는 동영상 썸네일을 터치합니다"}')

SUBTASK3_ID=$(echo $SUBTASK3 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

if [ -z "$SUBTASK1_ID" ] || [ -z "$SUBTASK2_ID" ] || [ -z "$SUBTASK3_ID" ]; then
  echo "❌ Failed to create subtasks"
  exit 1
fi

echo "✅ Created 3 subtasks:"
echo "   - Subtask 1 (ID: $SUBTASK1_ID): 1단계: 앱 열기"
echo "   - Subtask 2 (ID: $SUBTASK2_ID): 2단계: 검색하기"
echo "   - Subtask 3 (ID: $SUBTASK3_ID): 3단계: 동영상 재생"
echo ""

# 5. Create Session
echo "=== 5. Creating Session ==="
SESSION_RESPONSE=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE_ID/sessions/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "세션 제어 테스트"}')

SESSION_ID=$(echo $SESSION_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
SESSION_CODE=$(echo $SESSION_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['session_code'])" 2>/dev/null)
SESSION_STATUS=$(echo $SESSION_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['status'])" 2>/dev/null)

if [ -z "$SESSION_ID" ]; then
  echo "❌ Failed to create session"
  echo "Response: $SESSION_RESPONSE"
  exit 1
fi

echo "✅ Session created:"
echo "   - Session ID: $SESSION_ID"
echo "   - Session Code: $SESSION_CODE"
echo "   - Initial Status: $SESSION_STATUS"
echo ""

# Wait for user input
echo "Press Enter to start session control tests..."
read

# 6. Test Session Start
echo "=========================================="
echo "=== 6. Testing SESSION START ==="
echo "=========================================="
START_RESPONSE=$(curl -s -X POST http://localhost:8000/api/sessions/$SESSION_ID/start/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"first_subtask_id\": $SUBTASK1_ID, \"message\": \"수업을 시작합니다!\"}")

echo "$START_RESPONSE" | python3 -m json.tool

START_STATUS=$(echo $START_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', 'error'))" 2>/dev/null)

if [ "$START_STATUS" == "IN_PROGRESS" ]; then
  echo ""
  echo "✅ Session started successfully"
else
  echo ""
  echo "❌ Session start failed"
  echo "Response: $START_RESPONSE"
fi
echo ""
sleep 2

# 7. Test Next Step
echo "=========================================="
echo "=== 7. Testing NEXT STEP (to Subtask 2) ==="
echo "=========================================="
NEXT_RESPONSE=$(curl -s -X POST http://localhost:8000/api/sessions/$SESSION_ID/next-step/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"next_subtask_id\": $SUBTASK2_ID, \"message\": \"다음 단계로 이동합니다\"}")

echo "$NEXT_RESPONSE" | python3 -m json.tool

CURRENT_SUBTASK=$(echo $NEXT_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('current_subtask', {}).get('id', 'error'))" 2>/dev/null)

if [ "$CURRENT_SUBTASK" == "$SUBTASK2_ID" ]; then
  echo ""
  echo "✅ Next step executed successfully"
else
  echo ""
  echo "❌ Next step failed"
fi
echo ""
sleep 2

# 8. Test Pause
echo "=========================================="
echo "=== 8. Testing PAUSE ==="
echo "=========================================="
PAUSE_RESPONSE=$(curl -s -X POST http://localhost:8000/api/sessions/$SESSION_ID/pause/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "잠시 쉬는 시간입니다"}')

echo "$PAUSE_RESPONSE" | python3 -m json.tool

PAUSE_ACTION=$(echo $PAUSE_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('action', 'error'))" 2>/dev/null)

if [ "$PAUSE_ACTION" == "PAUSE" ]; then
  echo ""
  echo "✅ Session paused successfully"
else
  echo ""
  echo "❌ Pause failed"
fi
echo ""
sleep 2

# 9. Test Resume
echo "=========================================="
echo "=== 9. Testing RESUME ==="
echo "=========================================="
RESUME_RESPONSE=$(curl -s -X POST http://localhost:8000/api/sessions/$SESSION_ID/resume/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "수업을 다시 시작합니다"}')

echo "$RESUME_RESPONSE" | python3 -m json.tool

RESUME_ACTION=$(echo $RESUME_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('action', 'error'))" 2>/dev/null)

if [ "$RESUME_ACTION" == "RESUME" ]; then
  echo ""
  echo "✅ Session resumed successfully"
else
  echo ""
  echo "❌ Resume failed"
fi
echo ""
sleep 2

# 10. Test Next Step again (to Subtask 3)
echo "=========================================="
echo "=== 10. Testing NEXT STEP (to Subtask 3) ==="
echo "=========================================="
NEXT2_RESPONSE=$(curl -s -X POST http://localhost:8000/api/sessions/$SESSION_ID/next-step/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"next_subtask_id\": $SUBTASK3_ID, \"message\": \"마지막 단계입니다\"}")

echo "$NEXT2_RESPONSE" | python3 -m json.tool

CURRENT_SUBTASK2=$(echo $NEXT2_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('current_subtask', {}).get('id', 'error'))" 2>/dev/null)

if [ "$CURRENT_SUBTASK2" == "$SUBTASK3_ID" ]; then
  echo ""
  echo "✅ Next step executed successfully"
else
  echo ""
  echo "❌ Next step failed"
fi
echo ""
sleep 2

# 11. Test End
echo "=========================================="
echo "=== 11. Testing SESSION END ==="
echo "=========================================="
END_RESPONSE=$(curl -s -X POST http://localhost:8000/api/sessions/$SESSION_ID/end/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"message": "수업을 마치겠습니다. 수고하셨습니다!"}')

echo "$END_RESPONSE" | python3 -m json.tool

END_STATUS=$(echo $END_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin).get('status', 'error'))" 2>/dev/null)

if [ "$END_STATUS" == "REVIEW_MODE" ]; then
  echo ""
  echo "✅ Session ended successfully"
else
  echo ""
  echo "❌ End failed"
fi
echo ""

# 12. Verify Final State
echo "=========================================="
echo "=== 12. Verifying Final Session State ==="
echo "=========================================="
FINAL_STATE=$(curl -s -X GET http://localhost:8000/api/sessions/$SESSION_ID/current/ \
  -H "Authorization: Bearer $TOKEN")

echo "$FINAL_STATE" | python3 -m json.tool
echo ""

# Summary
echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo "Lecture ID: $LECTURE_ID"
echo "Task ID: $TASK_ID"
echo "Subtasks: $SUBTASK1_ID, $SUBTASK2_ID, $SUBTASK3_ID"
echo "Session ID: $SESSION_ID"
echo "Session Code: $SESSION_CODE"
echo ""
echo "All session control tests completed!"
echo "=========================================="
