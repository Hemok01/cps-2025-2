#!/bin/bash

# Sample Lectures Creation Script
# Creates 3 sample lectures with Tasks and Subtasks

echo "=========================================="
echo "Creating Sample Lectures"
echo "=========================================="
echo ""

# 1. Get instructor token
echo "=== 1. Getting Instructor Token ==="
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8000/api/token/ \
  -H "Content-Type: application/json" \
  -d '{"email": "instructor@test.com", "password": "test1234"}')

TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['access'])" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to get token"
  exit 1
fi

echo "✅ Token obtained"
echo ""

# ============================================
# Lecture 1: 유튜브 동영상 검색하기
# ============================================
echo "=========================================="
echo "Creating Lecture 1: 유튜브 동영상 검색하기"
echo "=========================================="

LECTURE1=$(curl -s -X POST http://localhost:8000/api/lectures/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "유튜브 동영상 검색하기",
    "description": "유튜브 앱을 사용하여 원하는 동영상을 검색하고 시청하는 방법을 배웁니다. 검색창 사용법부터 동영상 재생까지 단계별로 학습합니다.",
    "is_active": true
  }')

LECTURE1_ID=$(echo $LECTURE1 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)

if [ -z "$LECTURE1_ID" ]; then
  echo "❌ Failed to create Lecture 1"
  echo "$LECTURE1"
  exit 1
fi

echo "✅ Lecture 1 created (ID: $LECTURE1_ID)"

# Task 1-1: 유튜브 앱 실행하기
echo "  Creating Task 1-1: 유튜브 앱 실행하기"
TASK1_1=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE1_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "유튜브 앱 실행하기",
    "description": "스마트폰에서 유튜브 앱을 찾아 실행합니다",
    "order_index": 1
  }')

TASK1_1_ID=$(echo $TASK1_1 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK1_1_ID)"

# Subtasks for Task 1-1
curl -s -X POST http://localhost:8000/api/tasks/$TASK1_1_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "홈 화면에서 유튜브 아이콘 찾기",
    "description": "빨간색 재생 버튼 모양의 유튜브 아이콘을 찾습니다",
    "order_index": 1,
    "target_action": "NAVIGATE",
    "guide_text": "스마트폰 홈 화면에서 유튜브 앱 아이콘을 찾아주세요",
    "voice_guide_text": "홈 화면에서 빨간색 유튜브 아이콘을 찾아보세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK1_1_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "유튜브 앱 터치하기",
    "description": "유튜브 아이콘을 터치하여 앱을 실행합니다",
    "order_index": 2,
    "target_action": "CLICK",
    "target_element_hint": "YouTube 앱 아이콘",
    "guide_text": "유튜브 아이콘을 한 번 터치해주세요",
    "voice_guide_text": "유튜브 아이콘을 터치하세요"
  }' > /dev/null

echo "    ✅ 2 Subtasks created"

# Task 1-2: 동영상 검색하기
echo "  Creating Task 1-2: 동영상 검색하기"
TASK1_2=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE1_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "동영상 검색하기",
    "description": "검색창을 사용하여 원하는 동영상을 찾습니다",
    "order_index": 2
  }')

TASK1_2_ID=$(echo $TASK1_2 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK1_2_ID)"

# Subtasks for Task 1-2
curl -s -X POST http://localhost:8000/api/tasks/$TASK1_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "검색 버튼 터치하기",
    "description": "화면 상단의 돋보기 모양 검색 버튼을 터치합니다",
    "order_index": 1,
    "target_action": "CLICK",
    "target_element_hint": "검색 버튼 (돋보기 아이콘)",
    "guide_text": "화면 위쪽에 있는 돋보기 모양을 터치해주세요",
    "voice_guide_text": "상단의 검색 버튼을 터치하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK1_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "검색어 입력하기",
    "description": "검색창에 보고 싶은 동영상의 제목이나 키워드를 입력합니다",
    "order_index": 2,
    "target_action": "INPUT",
    "target_element_hint": "검색 입력창",
    "guide_text": "검색창에 원하는 내용을 입력해주세요",
    "voice_guide_text": "검색창에 보고 싶은 내용을 입력하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK1_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "검색 결과에서 동영상 선택하기",
    "description": "검색 결과 목록에서 보고 싶은 동영상을 선택합니다",
    "order_index": 3,
    "target_action": "CLICK",
    "target_element_hint": "동영상 썸네일",
    "guide_text": "목록에서 보고 싶은 동영상을 터치해주세요",
    "voice_guide_text": "원하는 동영상을 터치하세요"
  }' > /dev/null

echo "    ✅ 3 Subtasks created"
echo ""

# ============================================
# Lecture 2: 네이버 지도로 길찾기
# ============================================
echo "=========================================="
echo "Creating Lecture 2: 네이버 지도로 길찾기"
echo "=========================================="

LECTURE2=$(curl -s -X POST http://localhost:8000/api/lectures/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "네이버 지도로 길찾기",
    "description": "네이버 지도 앱을 사용하여 목적지를 검색하고 길찾기 기능을 사용하는 방법을 배웁니다. 현재 위치에서 원하는 장소까지의 경로를 확인할 수 있습니다.",
    "is_active": true
  }')

LECTURE2_ID=$(echo $LECTURE2 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "✅ Lecture 2 created (ID: $LECTURE2_ID)"

# Task 2-1: 네이버 지도 앱 실행하기
echo "  Creating Task 2-1: 네이버 지도 앱 실행하기"
TASK2_1=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE2_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "네이버 지도 앱 실행하기",
    "description": "네이버 지도 앱을 찾아 실행합니다",
    "order_index": 1
  }')

TASK2_1_ID=$(echo $TASK2_1 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK2_1_ID)"

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_1_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "네이버 지도 앱 아이콘 찾기",
    "description": "초록색 지도 모양의 네이버 지도 아이콘을 찾습니다",
    "order_index": 1,
    "target_action": "NAVIGATE",
    "guide_text": "초록색 지도 모양의 아이콘을 찾아주세요",
    "voice_guide_text": "네이버 지도 아이콘을 찾아보세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_1_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "앱 실행하기",
    "description": "네이버 지도 아이콘을 터치하여 앱을 실행합니다",
    "order_index": 2,
    "target_action": "CLICK",
    "target_element_hint": "네이버 지도 앱 아이콘",
    "guide_text": "아이콘을 터치해주세요",
    "voice_guide_text": "아이콘을 터치하세요"
  }' > /dev/null

echo "    ✅ 2 Subtasks created"

# Task 2-2: 목적지 검색하기
echo "  Creating Task 2-2: 목적지 검색하기"
TASK2_2=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE2_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "목적지 검색하기",
    "description": "가고 싶은 장소를 검색합니다",
    "order_index": 2
  }')

TASK2_2_ID=$(echo $TASK2_2 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK2_2_ID)"

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "검색창 터치하기",
    "description": "화면 상단의 검색창을 터치합니다",
    "order_index": 1,
    "target_action": "CLICK",
    "target_element_hint": "검색창",
    "guide_text": "화면 위쪽의 검색창을 터치해주세요",
    "voice_guide_text": "상단 검색창을 터치하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "장소 이름 입력하기",
    "description": "가고 싶은 장소의 이름이나 주소를 입력합니다",
    "order_index": 2,
    "target_action": "INPUT",
    "target_element_hint": "검색 입력창",
    "guide_text": "목적지 이름을 입력해주세요",
    "voice_guide_text": "가고 싶은 장소를 입력하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "검색 결과에서 장소 선택하기",
    "description": "검색 결과 목록에서 원하는 장소를 선택합니다",
    "order_index": 3,
    "target_action": "CLICK",
    "target_element_hint": "장소 목록 항목",
    "guide_text": "목록에서 가고 싶은 장소를 터치해주세요",
    "voice_guide_text": "원하는 장소를 터치하세요"
  }' > /dev/null

echo "    ✅ 3 Subtasks created"

# Task 2-3: 길찾기 시작하기
echo "  Creating Task 2-3: 길찾기 시작하기"
TASK2_3=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE2_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "길찾기 시작하기",
    "description": "선택한 장소까지의 경로를 확인합니다",
    "order_index": 3
  }')

TASK2_3_ID=$(echo $TASK2_3 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK2_3_ID)"

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_3_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "길찾기 버튼 터치하기",
    "description": "파란색 길찾기 버튼을 터치합니다",
    "order_index": 1,
    "target_action": "CLICK",
    "target_element_hint": "길찾기 버튼",
    "guide_text": "파란색 길찾기 버튼을 터치해주세요",
    "voice_guide_text": "길찾기 버튼을 터치하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK2_3_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "이동 수단 선택하기",
    "description": "도보, 대중교통, 자동차 중 원하는 이동 수단을 선택합니다",
    "order_index": 2,
    "target_action": "CLICK",
    "target_element_hint": "이동 수단 버튼",
    "guide_text": "원하는 이동 수단을 선택해주세요",
    "voice_guide_text": "이동 수단을 선택하세요"
  }' > /dev/null

echo "    ✅ 2 Subtasks created"
echo ""

# ============================================
# Lecture 3: 인스타그램 게시물 작성하기
# ============================================
echo "=========================================="
echo "Creating Lecture 3: 인스타그램 게시물 작성하기"
echo "=========================================="

LECTURE3=$(curl -s -X POST http://localhost:8000/api/lectures/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "인스타그램 게시물 작성하기",
    "description": "인스타그램에 사진과 함께 게시물을 작성하고 공유하는 방법을 배웁니다. 사진 선택부터 해시태그 작성, 게시까지 전체 과정을 학습합니다.",
    "is_active": true
  }')

LECTURE3_ID=$(echo $LECTURE3 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "✅ Lecture 3 created (ID: $LECTURE3_ID)"

# Task 3-1: 인스타그램 앱 실행하기
echo "  Creating Task 3-1: 인스타그램 앱 실행하기"
TASK3_1=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE3_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "인스타그램 앱 실행하기",
    "description": "인스타그램 앱을 찾아 실행합니다",
    "order_index": 1
  }')

TASK3_1_ID=$(echo $TASK3_1 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK3_1_ID)"

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_1_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "인스타그램 아이콘 찾기",
    "description": "그라데이션 카메라 모양의 인스타그램 아이콘을 찾습니다",
    "order_index": 1,
    "target_action": "NAVIGATE",
    "guide_text": "그라데이션 색상의 카메라 모양 아이콘을 찾아주세요",
    "voice_guide_text": "인스타그램 아이콘을 찾아보세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_1_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "앱 실행하기",
    "description": "인스타그램 아이콘을 터치하여 앱을 실행합니다",
    "order_index": 2,
    "target_action": "CLICK",
    "target_element_hint": "Instagram 앱 아이콘",
    "guide_text": "아이콘을 터치해주세요",
    "voice_guide_text": "아이콘을 터치하세요"
  }' > /dev/null

echo "    ✅ 2 Subtasks created"

# Task 3-2: 새 게시물 만들기
echo "  Creating Task 3-2: 새 게시물 만들기"
TASK3_2=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE3_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "새 게시물 만들기",
    "description": "새 게시물 작성 화면을 엽니다",
    "order_index": 2
  }')

TASK3_2_ID=$(echo $TASK3_2 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK3_2_ID)"

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "추가 버튼 터치하기",
    "description": "화면 하단 중앙의 + 버튼을 터치합니다",
    "order_index": 1,
    "target_action": "CLICK",
    "target_element_hint": "+ 버튼",
    "guide_text": "하단 중앙의 + 버튼을 터치해주세요",
    "voice_guide_text": "플러스 버튼을 터치하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_2_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "게시물 선택하기",
    "description": "게시물 종류 중 일반 게시물을 선택합니다",
    "order_index": 2,
    "target_action": "CLICK",
    "target_element_hint": "게시물 버튼",
    "guide_text": "게시물을 선택해주세요",
    "voice_guide_text": "게시물을 선택하세요"
  }' > /dev/null

echo "    ✅ 2 Subtasks created"

# Task 3-3: 사진 선택하고 게시하기
echo "  Creating Task 3-3: 사진 선택하고 게시하기"
TASK3_3=$(curl -s -X POST http://localhost:8000/api/lectures/$LECTURE3_ID/tasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "사진 선택하고 게시하기",
    "description": "갤러리에서 사진을 선택하고 게시물을 작성합니다",
    "order_index": 3
  }')

TASK3_3_ID=$(echo $TASK3_3 | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
echo "    ✅ Task created (ID: $TASK3_3_ID)"

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_3_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "갤러리에서 사진 선택하기",
    "description": "갤러리에서 게시할 사진을 선택합니다",
    "order_index": 1,
    "target_action": "CLICK",
    "target_element_hint": "사진 썸네일",
    "guide_text": "갤러리에서 원하는 사진을 터치해주세요",
    "voice_guide_text": "사진을 선택하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_3_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "다음 버튼 터치하기",
    "description": "화면 오른쪽 위의 다음 버튼을 터치합니다",
    "order_index": 2,
    "target_action": "CLICK",
    "target_element_hint": "다음 버튼",
    "guide_text": "오른쪽 위의 다음 버튼을 터치해주세요",
    "voice_guide_text": "다음 버튼을 터치하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_3_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "문구 작성하기",
    "description": "게시물에 포함할 문구나 해시태그를 작성합니다",
    "order_index": 3,
    "target_action": "INPUT",
    "target_element_hint": "문구 입력창",
    "guide_text": "문구 입력란에 내용을 작성해주세요",
    "voice_guide_text": "문구를 작성하세요"
  }' > /dev/null

curl -s -X POST http://localhost:8000/api/tasks/$TASK3_3_ID/subtasks/create/ \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "공유 버튼 터치하기",
    "description": "공유 버튼을 터치하여 게시물을 업로드합니다",
    "order_index": 4,
    "target_action": "CLICK",
    "target_element_hint": "공유 버튼",
    "guide_text": "공유 버튼을 터치해주세요",
    "voice_guide_text": "공유 버튼을 터치하세요"
  }' > /dev/null

echo "    ✅ 4 Subtasks created"
echo ""

# ============================================
# Summary
# ============================================
echo "=========================================="
echo "Summary"
echo "=========================================="
echo ""
echo "✅ 3 Sample lectures created successfully!"
echo ""
echo "Lecture 1: 유튜브 동영상 검색하기 (ID: $LECTURE1_ID)"
echo "  - 2 Tasks, 5 Subtasks total"
echo ""
echo "Lecture 2: 네이버 지도로 길찾기 (ID: $LECTURE2_ID)"
echo "  - 3 Tasks, 7 Subtasks total"
echo ""
echo "Lecture 3: 인스타그램 게시물 작성하기 (ID: $LECTURE3_ID)"
echo "  - 3 Tasks, 8 Subtasks total"
echo ""
echo "=========================================="
echo "You can now view these lectures at:"
echo "GET http://localhost:8000/api/lectures/"
echo "=========================================="
