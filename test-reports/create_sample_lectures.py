#!/usr/bin/env python3
"""
Sample Lectures Creation Script
Creates 3 sample lectures with Tasks and Subtasks
"""

import requests
import json
import sys

BASE_URL = "http://localhost:8000"

def get_token():
    """Get instructor authentication token"""
    response = requests.post(
        f"{BASE_URL}/api/token/",
        json={"email": "instructor@test.com", "password": "test1234"}
    )
    if response.status_code == 200:
        return response.json()['access']
    else:
        print(f"❌ Failed to get token: {response.text}")
        sys.exit(1)

def create_lecture(token, title, description):
    """Create a lecture"""
    response = requests.post(
        f"{BASE_URL}/api/lectures/",
        headers={"Authorization": f"Bearer {token}"},
        json={
            "title": title,
            "description": description,
            "is_active": True
        }
    )
    if response.status_code == 201:
        return response.json()
    else:
        print(f"❌ Failed to create lecture '{title}': {response.text}")
        return None

def create_task(token, lecture_id, title, description, order_index):
    """Create a task for a lecture"""
    response = requests.post(
        f"{BASE_URL}/api/lectures/{lecture_id}/tasks/create/",
        headers={"Authorization": f"Bearer {token}"},
        json={
            "title": title,
            "description": description,
            "order_index": order_index
        }
    )
    if response.status_code == 201:
        return response.json()
    else:
        print(f"❌ Failed to create task '{title}': {response.text}")
        return None

def create_subtask(token, task_id, data):
    """Create a subtask for a task"""
    response = requests.post(
        f"{BASE_URL}/api/tasks/{task_id}/subtasks/create/",
        headers={"Authorization": f"Bearer {token}"},
        json=data
    )
    if response.status_code == 201:
        return response.json()
    else:
        print(f"❌ Failed to create subtask: {response.text}")
        return None

def main():
    print("=" * 50)
    print("Creating Sample Lectures")
    print("=" * 50)
    print()

    # Get token
    print("=== Getting Instructor Token ===")
    token = get_token()
    print("✅ Token obtained")
    print()

    # ============================================
    # Lecture 1: 유튜브 동영상 검색하기
    # ============================================
    print("=" * 50)
    print("Creating Lecture 1: 유튜브 동영상 검색하기")
    print("=" * 50)

    lecture1 = create_lecture(
        token,
        "유튜브 동영상 검색하기",
        "유튜브 앱을 사용하여 원하는 동영상을 검색하고 시청하는 방법을 배웁니다. 검색창 사용법부터 동영상 재생까지 단계별로 학습합니다."
    )
    if not lecture1:
        sys.exit(1)

    print(f"✅ Lecture 1 created (ID: {lecture1['id']})")

    # Task 1-1
    print("  Creating Task 1-1: 유튜브 앱 실행하기")
    task1_1 = create_task(
        token, lecture1['id'],
        "유튜브 앱 실행하기",
        "스마트폰에서 유튜브 앱을 찾아 실행합니다",
        1
    )
    print(f"    ✅ Task created (ID: {task1_1['id']})")

    subtasks_1_1 = [
        {
            "title": "홈 화면에서 유튜브 아이콘 찾기",
            "description": "빨간색 재생 버튼 모양의 유튜브 아이콘을 찾습니다",
            "order_index": 1,
            "target_action": "NAVIGATE",
            "guide_text": "스마트폰 홈 화면에서 유튜브 앱 아이콘을 찾아주세요",
            "voice_guide_text": "홈 화면에서 빨간색 유튜브 아이콘을 찾아보세요"
        },
        {
            "title": "유튜브 앱 터치하기",
            "description": "유튜브 아이콘을 터치하여 앱을 실행합니다",
            "order_index": 2,
            "target_action": "CLICK",
            "target_element_hint": "YouTube 앱 아이콘",
            "guide_text": "유튜브 아이콘을 한 번 터치해주세요",
            "voice_guide_text": "유튜브 아이콘을 터치하세요"
        }
    ]

    for subtask in subtasks_1_1:
        create_subtask(token, task1_1['id'], subtask)

    print(f"    ✅ {len(subtasks_1_1)} Subtasks created")

    # Task 1-2
    print("  Creating Task 1-2: 동영상 검색하기")
    task1_2 = create_task(
        token, lecture1['id'],
        "동영상 검색하기",
        "검색창을 사용하여 원하는 동영상을 찾습니다",
        2
    )
    print(f"    ✅ Task created (ID: {task1_2['id']})")

    subtasks_1_2 = [
        {
            "title": "검색 버튼 터치하기",
            "description": "화면 상단의 돋보기 모양 검색 버튼을 터치합니다",
            "order_index": 1,
            "target_action": "CLICK",
            "target_element_hint": "검색 버튼 (돋보기 아이콘)",
            "guide_text": "화면 위쪽에 있는 돋보기 모양을 터치해주세요",
            "voice_guide_text": "상단의 검색 버튼을 터치하세요"
        },
        {
            "title": "검색어 입력하기",
            "description": "검색창에 보고 싶은 동영상의 제목이나 키워드를 입력합니다",
            "order_index": 2,
            "target_action": "INPUT",
            "target_element_hint": "검색 입력창",
            "guide_text": "검색창에 원하는 내용을 입력해주세요",
            "voice_guide_text": "검색창에 보고 싶은 내용을 입력하세요"
        },
        {
            "title": "검색 결과에서 동영상 선택하기",
            "description": "검색 결과 목록에서 보고 싶은 동영상을 선택합니다",
            "order_index": 3,
            "target_action": "CLICK",
            "target_element_hint": "동영상 썸네일",
            "guide_text": "목록에서 보고 싶은 동영상을 터치해주세요",
            "voice_guide_text": "원하는 동영상을 터치하세요"
        }
    ]

    for subtask in subtasks_1_2:
        create_subtask(token, task1_2['id'], subtask)

    print(f"    ✅ {len(subtasks_1_2)} Subtasks created")
    print()

    # ============================================
    # Lecture 2: 네이버 지도로 길찾기
    # ============================================
    print("=" * 50)
    print("Creating Lecture 2: 네이버 지도로 길찾기")
    print("=" * 50)

    lecture2 = create_lecture(
        token,
        "네이버 지도로 길찾기",
        "네이버 지도 앱을 사용하여 목적지를 검색하고 길찾기 기능을 사용하는 방법을 배웁니다. 현재 위치에서 원하는 장소까지의 경로를 확인할 수 있습니다."
    )
    print(f"✅ Lecture 2 created (ID: {lecture2['id']})")

    # Task 2-1
    print("  Creating Task 2-1: 네이버 지도 앱 실행하기")
    task2_1 = create_task(
        token, lecture2['id'],
        "네이버 지도 앱 실행하기",
        "네이버 지도 앱을 찾아 실행합니다",
        1
    )
    print(f"    ✅ Task created (ID: {task2_1['id']})")

    subtasks_2_1 = [
        {
            "title": "네이버 지도 앱 아이콘 찾기",
            "description": "초록색 지도 모양의 네이버 지도 아이콘을 찾습니다",
            "order_index": 1,
            "target_action": "NAVIGATE",
            "guide_text": "초록색 지도 모양의 아이콘을 찾아주세요",
            "voice_guide_text": "네이버 지도 아이콘을 찾아보세요"
        },
        {
            "title": "앱 실행하기",
            "description": "네이버 지도 아이콘을 터치하여 앱을 실행합니다",
            "order_index": 2,
            "target_action": "CLICK",
            "target_element_hint": "네이버 지도 앱 아이콘",
            "guide_text": "아이콘을 터치해주세요",
            "voice_guide_text": "아이콘을 터치하세요"
        }
    ]

    for subtask in subtasks_2_1:
        create_subtask(token, task2_1['id'], subtask)

    print(f"    ✅ {len(subtasks_2_1)} Subtasks created")

    # Task 2-2
    print("  Creating Task 2-2: 목적지 검색하기")
    task2_2 = create_task(
        token, lecture2['id'],
        "목적지 검색하기",
        "가고 싶은 장소를 검색합니다",
        2
    )
    print(f"    ✅ Task created (ID: {task2_2['id']})")

    subtasks_2_2 = [
        {
            "title": "검색창 터치하기",
            "description": "화면 상단의 검색창을 터치합니다",
            "order_index": 1,
            "target_action": "CLICK",
            "target_element_hint": "검색창",
            "guide_text": "화면 위쪽의 검색창을 터치해주세요",
            "voice_guide_text": "상단 검색창을 터치하세요"
        },
        {
            "title": "장소 이름 입력하기",
            "description": "가고 싶은 장소의 이름이나 주소를 입력합니다",
            "order_index": 2,
            "target_action": "INPUT",
            "target_element_hint": "검색 입력창",
            "guide_text": "목적지 이름을 입력해주세요",
            "voice_guide_text": "가고 싶은 장소를 입력하세요"
        },
        {
            "title": "검색 결과에서 장소 선택하기",
            "description": "검색 결과 목록에서 원하는 장소를 선택합니다",
            "order_index": 3,
            "target_action": "CLICK",
            "target_element_hint": "장소 목록 항목",
            "guide_text": "목록에서 가고 싶은 장소를 터치해주세요",
            "voice_guide_text": "원하는 장소를 터치하세요"
        }
    ]

    for subtask in subtasks_2_2:
        create_subtask(token, task2_2['id'], subtask)

    print(f"    ✅ {len(subtasks_2_2)} Subtasks created")

    # Task 2-3
    print("  Creating Task 2-3: 길찾기 시작하기")
    task2_3 = create_task(
        token, lecture2['id'],
        "길찾기 시작하기",
        "선택한 장소까지의 경로를 확인합니다",
        3
    )
    print(f"    ✅ Task created (ID: {task2_3['id']})")

    subtasks_2_3 = [
        {
            "title": "길찾기 버튼 터치하기",
            "description": "파란색 길찾기 버튼을 터치합니다",
            "order_index": 1,
            "target_action": "CLICK",
            "target_element_hint": "길찾기 버튼",
            "guide_text": "파란색 길찾기 버튼을 터치해주세요",
            "voice_guide_text": "길찾기 버튼을 터치하세요"
        },
        {
            "title": "이동 수단 선택하기",
            "description": "도보, 대중교통, 자동차 중 원하는 이동 수단을 선택합니다",
            "order_index": 2,
            "target_action": "CLICK",
            "target_element_hint": "이동 수단 버튼",
            "guide_text": "원하는 이동 수단을 선택해주세요",
            "voice_guide_text": "이동 수단을 선택하세요"
        }
    ]

    for subtask in subtasks_2_3:
        create_subtask(token, task2_3['id'], subtask)

    print(f"    ✅ {len(subtasks_2_3)} Subtasks created")
    print()

    # ============================================
    # Lecture 3: 인스타그램 게시물 작성하기
    # ============================================
    print("=" * 50)
    print("Creating Lecture 3: 인스타그램 게시물 작성하기")
    print("=" * 50)

    lecture3 = create_lecture(
        token,
        "인스타그램 게시물 작성하기",
        "인스타그램에 사진과 함께 게시물을 작성하고 공유하는 방법을 배웁니다. 사진 선택부터 해시태그 작성, 게시까지 전체 과정을 학습합니다."
    )
    print(f"✅ Lecture 3 created (ID: {lecture3['id']})")

    # Task 3-1
    print("  Creating Task 3-1: 인스타그램 앱 실행하기")
    task3_1 = create_task(
        token, lecture3['id'],
        "인스타그램 앱 실행하기",
        "인스타그램 앱을 찾아 실행합니다",
        1
    )
    print(f"    ✅ Task created (ID: {task3_1['id']})")

    subtasks_3_1 = [
        {
            "title": "인스타그램 아이콘 찾기",
            "description": "그라데이션 카메라 모양의 인스타그램 아이콘을 찾습니다",
            "order_index": 1,
            "target_action": "NAVIGATE",
            "guide_text": "그라데이션 색상의 카메라 모양 아이콘을 찾아주세요",
            "voice_guide_text": "인스타그램 아이콘을 찾아보세요"
        },
        {
            "title": "앱 실행하기",
            "description": "인스타그램 아이콘을 터치하여 앱을 실행합니다",
            "order_index": 2,
            "target_action": "CLICK",
            "target_element_hint": "Instagram 앱 아이콘",
            "guide_text": "아이콘을 터치해주세요",
            "voice_guide_text": "아이콘을 터치하세요"
        }
    ]

    for subtask in subtasks_3_1:
        create_subtask(token, task3_1['id'], subtask)

    print(f"    ✅ {len(subtasks_3_1)} Subtasks created")

    # Task 3-2
    print("  Creating Task 3-2: 새 게시물 만들기")
    task3_2 = create_task(
        token, lecture3['id'],
        "새 게시물 만들기",
        "새 게시물 작성 화면을 엽니다",
        2
    )
    print(f"    ✅ Task created (ID: {task3_2['id']})")

    subtasks_3_2 = [
        {
            "title": "추가 버튼 터치하기",
            "description": "화면 하단 중앙의 + 버튼을 터치합니다",
            "order_index": 1,
            "target_action": "CLICK",
            "target_element_hint": "+ 버튼",
            "guide_text": "하단 중앙의 + 버튼을 터치해주세요",
            "voice_guide_text": "플러스 버튼을 터치하세요"
        },
        {
            "title": "게시물 선택하기",
            "description": "게시물 종류 중 일반 게시물을 선택합니다",
            "order_index": 2,
            "target_action": "CLICK",
            "target_element_hint": "게시물 버튼",
            "guide_text": "게시물을 선택해주세요",
            "voice_guide_text": "게시물을 선택하세요"
        }
    ]

    for subtask in subtasks_3_2:
        create_subtask(token, task3_2['id'], subtask)

    print(f"    ✅ {len(subtasks_3_2)} Subtasks created")

    # Task 3-3
    print("  Creating Task 3-3: 사진 선택하고 게시하기")
    task3_3 = create_task(
        token, lecture3['id'],
        "사진 선택하고 게시하기",
        "갤러리에서 사진을 선택하고 게시물을 작성합니다",
        3
    )
    print(f"    ✅ Task created (ID: {task3_3['id']})")

    subtasks_3_3 = [
        {
            "title": "갤러리에서 사진 선택하기",
            "description": "갤러리에서 게시할 사진을 선택합니다",
            "order_index": 1,
            "target_action": "CLICK",
            "target_element_hint": "사진 썸네일",
            "guide_text": "갤러리에서 원하는 사진을 터치해주세요",
            "voice_guide_text": "사진을 선택하세요"
        },
        {
            "title": "다음 버튼 터치하기",
            "description": "화면 오른쪽 위의 다음 버튼을 터치합니다",
            "order_index": 2,
            "target_action": "CLICK",
            "target_element_hint": "다음 버튼",
            "guide_text": "오른쪽 위의 다음 버튼을 터치해주세요",
            "voice_guide_text": "다음 버튼을 터치하세요"
        },
        {
            "title": "문구 작성하기",
            "description": "게시물에 포함할 문구나 해시태그를 작성합니다",
            "order_index": 3,
            "target_action": "INPUT",
            "target_element_hint": "문구 입력창",
            "guide_text": "문구 입력란에 내용을 작성해주세요",
            "voice_guide_text": "문구를 작성하세요"
        },
        {
            "title": "공유 버튼 터치하기",
            "description": "공유 버튼을 터치하여 게시물을 업로드합니다",
            "order_index": 4,
            "target_action": "CLICK",
            "target_element_hint": "공유 버튼",
            "guide_text": "공유 버튼을 터치해주세요",
            "voice_guide_text": "공유 버튼을 터치하세요"
        }
    ]

    for subtask in subtasks_3_3:
        create_subtask(token, task3_3['id'], subtask)

    print(f"    ✅ {len(subtasks_3_3)} Subtasks created")
    print()

    # Summary
    print("=" * 50)
    print("Summary")
    print("=" * 50)
    print()
    print("✅ 3 Sample lectures created successfully!")
    print()
    print(f"Lecture 1: 유튜브 동영상 검색하기 (ID: {lecture1['id']})")
    print("  - 2 Tasks, 5 Subtasks total")
    print()
    print(f"Lecture 2: 네이버 지도로 길찾기 (ID: {lecture2['id']})")
    print("  - 3 Tasks, 7 Subtasks total")
    print()
    print(f"Lecture 3: 인스타그램 게시물 작성하기 (ID: {lecture3['id']})")
    print("  - 3 Tasks, 8 Subtasks total")
    print()
    print("=" * 50)
    print("You can now view these lectures at:")
    print("GET http://localhost:8000/api/lectures/")
    print("=" * 50)


if __name__ == "__main__":
    main()
