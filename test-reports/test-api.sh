#!/bin/bash

# API 테스트 스크립트
# 사용법: ./test-api.sh

echo "======================================"
echo "프론트엔드-백엔드 API 통합 테스트"
echo "======================================"
echo ""

# 색상 정의
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 백엔드 URL
API_URL="http://localhost:8000/api"

# 테스트 계정
EMAIL="instructor@test.com"
PASSWORD="test1234"

echo "1. 로그인 테스트..."
LOGIN_RESPONSE=$(curl -s -X POST "$API_URL/token/" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$EMAIL\", \"password\": \"$PASSWORD\"}")

if echo "$LOGIN_RESPONSE" | grep -q "access"; then
  echo -e "${GREEN}✓ 로그인 성공${NC}"
  TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"access":"[^"]*"' | cut -d'"' -f4)
  echo "토큰: ${TOKEN:0:50}..."
else
  echo -e "${RED}✗ 로그인 실패${NC}"
  echo "$LOGIN_RESPONSE"
  exit 1
fi

echo ""
echo "2. 사용자 정보 조회..."
USER_RESPONSE=$(curl -s -X GET "$API_URL/auth/me/" \
  -H "Authorization: Bearer $TOKEN")

if echo "$USER_RESPONSE" | grep -q "email"; then
  echo -e "${GREEN}✓ 사용자 정보 조회 성공${NC}"
  echo "$USER_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$USER_RESPONSE"
else
  echo -e "${RED}✗ 사용자 정보 조회 실패${NC}"
  echo "$USER_RESPONSE"
fi

echo ""
echo "3. 강의 목록 조회..."
LECTURES_RESPONSE=$(curl -s -X GET "$API_URL/lectures/" \
  -H "Authorization: Bearer $TOKEN")

if echo "$LECTURES_RESPONSE" | grep -q "count"; then
  LECTURE_COUNT=$(echo "$LECTURES_RESPONSE" | grep -o '"count":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✓ 강의 목록 조회 성공 (총 ${LECTURE_COUNT}개)${NC}"

  # 첫 번째 강의 ID 추출
  LECTURE_ID=$(echo "$LECTURES_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
  echo "첫 번째 강의 ID: $LECTURE_ID"
else
  echo -e "${RED}✗ 강의 목록 조회 실패${NC}"
  echo "$LECTURES_RESPONSE"
fi

if [ ! -z "$LECTURE_ID" ]; then
  echo ""
  echo "4. 세션 생성..."
  SESSION_RESPONSE=$(curl -s -X POST "$API_URL/lectures/$LECTURE_ID/sessions/create/" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"title": "자동 테스트 세션"}')

  if echo "$SESSION_RESPONSE" | grep -q "session_code"; then
    SESSION_CODE=$(echo "$SESSION_RESPONSE" | grep -o '"session_code":"[^"]*"' | cut -d'"' -f4)
    SESSION_ID=$(echo "$SESSION_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
    echo -e "${GREEN}✓ 세션 생성 성공${NC}"
    echo "세션 ID: $SESSION_ID"
    echo "세션 코드: $SESSION_CODE"
  else
    echo -e "${RED}✗ 세션 생성 실패${NC}"
    echo "$SESSION_RESPONSE"
  fi
fi

echo ""
echo "5. 헬스 체크..."
HEALTH_RESPONSE=$(curl -s -X GET "$API_URL/health/")

if echo "$HEALTH_RESPONSE" | grep -q "status"; then
  echo -e "${GREEN}✓ 헬스 체크 성공${NC}"
  echo "$HEALTH_RESPONSE"
else
  echo -e "${RED}✗ 헬스 체크 실패${NC}"
  echo "$HEALTH_RESPONSE"
fi

echo ""
echo "======================================"
echo "테스트 완료"
echo "======================================"
