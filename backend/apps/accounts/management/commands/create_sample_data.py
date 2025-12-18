"""
통계 페이지 테스트를 위한 샘플 데이터 생성 커맨드

사용법:
  python manage.py create_sample_data           # 새 데이터 생성 (기존 데이터 유지)
  python manage.py create_sample_data --reset   # 기존 샘플 데이터 삭제 후 새로 생성
"""
from django.core.management.base import BaseCommand
from django.contrib.auth import get_user_model
from django.utils import timezone
from datetime import timedelta
import random

from apps.lectures.models import Lecture, UserLectureEnrollment
from apps.tasks.models import Task, Subtask
from apps.progress.models import UserProgress
from apps.help.models import HelpRequest
from apps.sessions.models import LectureSession, SessionParticipant

User = get_user_model()


class Command(BaseCommand):
    help = '통계 페이지 테스트를 위한 샘플 데이터 생성'

    def add_arguments(self, parser):
        parser.add_argument(
            '--reset',
            action='store_true',
            help='기존 샘플 데이터를 삭제하고 새로 생성합니다',
        )

    def handle(self, *args, **options):
        reset = options.get('reset', False)

        if reset:
            self.stdout.write(self.style.WARNING('기존 샘플 데이터 삭제 중...'))
            self._reset_sample_data()

        self.stdout.write('샘플 데이터 생성 시작...')
        self._create_sample_data()

    def _reset_sample_data(self):
        """기존 샘플 데이터 삭제"""
        # 샘플 강의 삭제 (cascade로 관련 데이터도 삭제됨)
        lecture = Lecture.objects.filter(title='유튜브 사용법 배우기').first()
        if lecture:
            # 관련 데이터 삭제
            HelpRequest.objects.filter(subtask__task__lecture=lecture).delete()
            UserProgress.objects.filter(subtask__task__lecture=lecture).delete()
            SessionParticipant.objects.filter(session__lecture=lecture).delete()
            LectureSession.objects.filter(lecture=lecture).delete()
            UserLectureEnrollment.objects.filter(lecture=lecture).delete()
            Subtask.objects.filter(task__lecture=lecture).delete()
            Task.objects.filter(lecture=lecture).delete()
            lecture.delete()
            self.stdout.write(self.style.SUCCESS('기존 강의 및 관련 데이터 삭제 완료'))

        # 샘플 학생 계정의 진행 데이터만 삭제 (계정은 유지)
        for i in range(1, 11):
            student = User.objects.filter(email=f'student{i}@test.com').first()
            if student:
                UserProgress.objects.filter(user=student).delete()
                HelpRequest.objects.filter(user=student).delete()
                SessionParticipant.objects.filter(user=student).delete()

        self.stdout.write(self.style.SUCCESS('샘플 데이터 초기화 완료'))

    def _create_sample_data(self):
        """샘플 데이터 생성"""
        # 1. 강사 계정 확인/생성
        instructor, created = User.objects.get_or_create(
            email='instructor@test.com',
            defaults={
                'name': 'Test Instructor',
                'role': 'INSTRUCTOR',
            }
        )
        if created:
            instructor.set_password('test1234')
            instructor.save()
            self.stdout.write(self.style.SUCCESS('강사 계정 생성: instructor@test.com'))
        else:
            self.stdout.write('강사 계정 이미 존재: instructor@test.com')

        # 2. 학생 계정 생성
        students = []
        student_names = [
            '김민수', '이영희', '박철수', '최수진', '정대현',
            '한미영', '오준혁', '강소연', '임태웅', '윤서아'
        ]
        for i, name in enumerate(student_names):
            student, created = User.objects.get_or_create(
                email=f'student{i+1}@test.com',
                defaults={
                    'name': name,
                    'role': 'STUDENT',
                }
            )
            if created:
                student.set_password('test1234')
                student.save()
            students.append(student)
        self.stdout.write(self.style.SUCCESS(f'학생 계정 {len(students)}명 준비됨'))

        # 3. 강의 생성
        lecture, created = Lecture.objects.get_or_create(
            instructor=instructor,
            title='유튜브 사용법 배우기',
            defaults={
                'description': '유튜브 앱을 사용하여 동영상을 검색하고 시청하는 방법을 배웁니다.',
                'is_active': True,
            }
        )
        if created:
            self.stdout.write(self.style.SUCCESS(f'강의 생성: {lecture.title}'))
        else:
            self.stdout.write(f'강의 이미 존재: {lecture.title}')

        # 4. 태스크 및 서브태스크 생성
        tasks_data = [
            {
                'title': '유튜브 앱 열기',
                'subtasks': [
                    ('홈 화면으로 이동', 'NAVIGATE', '홈 버튼을 눌러 홈 화면으로 이동합니다'),
                    ('유튜브 앱 아이콘 찾기', 'SCROLL', '화면을 스크롤하여 유튜브 앱을 찾습니다'),
                    ('유튜브 앱 실행', 'CLICK', '유튜브 앱 아이콘을 탭합니다'),
                ]
            },
            {
                'title': '동영상 검색하기',
                'subtasks': [
                    ('검색 아이콘 찾기', 'NAVIGATE', '화면 상단의 돋보기 아이콘을 찾습니다'),
                    ('검색창 탭하기', 'CLICK', '돋보기 아이콘을 탭합니다'),
                    ('검색어 입력', 'INPUT', '원하는 검색어를 입력합니다'),
                    ('검색 버튼 누르기', 'CLICK', '키보드의 검색 버튼을 누릅니다'),
                ]
            },
            {
                'title': '동영상 시청하기',
                'subtasks': [
                    ('동영상 선택', 'CLICK', '원하는 동영상 썸네일을 탭합니다'),
                    ('전체 화면 보기', 'CLICK', '전체 화면 버튼을 누릅니다'),
                    ('볼륨 조절', 'CLICK', '볼륨 버튼으로 소리를 조절합니다'),
                    ('영상 일시정지', 'CLICK', '화면 중앙을 탭하여 일시정지합니다'),
                    ('영상 재개', 'CLICK', '다시 재생 버튼을 누릅니다'),
                ]
            },
        ]

        subtasks_list = []
        global_order = 0
        for task_idx, task_data in enumerate(tasks_data):
            task, _ = Task.objects.get_or_create(
                lecture=lecture,
                order_index=task_idx,
                defaults={
                    'title': task_data['title'],
                    'description': f'{task_data["title"]} 관련 단계입니다.',
                }
            )

            for sub_idx, (title, action, guide) in enumerate(task_data['subtasks']):
                subtask, _ = Subtask.objects.get_or_create(
                    task=task,
                    order_index=global_order,
                    defaults={
                        'title': title,
                        'description': guide,
                        'target_action': action,
                        'guide_text': guide,
                    }
                )
                subtasks_list.append(subtask)
                global_order += 1

        self.stdout.write(self.style.SUCCESS(f'태스크 {len(tasks_data)}개, 서브태스크 {len(subtasks_list)}개 준비됨'))

        # 5. 학생 수강 등록
        for student in students:
            UserLectureEnrollment.objects.get_or_create(
                user=student,
                lecture=lecture
            )
        self.stdout.write(self.style.SUCCESS(f'학생 {len(students)}명 수강 등록'))

        # 6. 과거 종료된 세션 생성 (세션 비교용)
        now = timezone.now()
        sessions = []

        # 도움 요청률: 40%대에서 점차 감소하는 추세
        help_request_rates = [0.45, 0.35, 0.25, 0.18]  # 45%, 35%, 25%, 18%

        for i in range(4):  # 4개 세션
            session_date = now - timedelta(days=(4-i)*7)  # 4주전, 3주전, 2주전, 1주전
            session, created = LectureSession.objects.get_or_create(
                lecture=lecture,
                instructor=instructor,
                title=f'제{i+1}차 실습 세션',
                defaults={
                    'status': 'ENDED',
                    'started_at': session_date,
                    'ended_at': session_date + timedelta(hours=2),
                }
            )
            sessions.append(session)

            if created:
                # 참가자 생성 (점점 더 많은 학생이 참가)
                participant_count = 6 + i * 1  # 6, 7, 8, 9명
                for j, student in enumerate(students[:participant_count]):
                    # 완료 비율을 세션마다 다르게 (개선 추세) - 80% 목표
                    is_completed = random.random() < (0.65 + i * 0.1)  # 65%, 75%, 85%, 95%

                    # 평균 소요 시간: 2시간(120분) 안팎으로 설정
                    completion_minutes = random.randint(100, 140)  # 100~140분

                    # 참가자 생성
                    participant, p_created = SessionParticipant.objects.get_or_create(
                        session=session,
                        user=student,
                        defaults={
                            'display_name': student.name,
                            'status': 'COMPLETED' if is_completed else 'ACTIVE',
                            'current_subtask': subtasks_list[-1] if is_completed else subtasks_list[random.randint(0, len(subtasks_list)-1)],
                            'completed_at': session_date + timedelta(minutes=completion_minutes) if is_completed else None,
                        }
                    )

                    # auto_now_add 필드는 update()로 수정해야 함
                    if p_created:
                        SessionParticipant.objects.filter(pk=participant.pk).update(
                            joined_at=session_date
                        )

                # 세션별 도움 요청 생성 (도움 요청률 감소 추세)
                help_rate = help_request_rates[i]
                help_count = max(int(participant_count * help_rate), 2)  # 최소 2개 이상
                for k in range(help_count):
                    random_student = students[random.randint(0, participant_count - 1)]
                    random_subtask = random.choice(subtasks_list)
                    request_time = session_date + timedelta(minutes=random.randint(10, 100))

                    # HelpRequest 생성 후 created_at 수정 (auto_now_add 때문에)
                    help_request = HelpRequest.objects.create(
                        user=random_student,
                        subtask=random_subtask,
                        request_type=random.choice(['MANUAL', 'AUTO']),
                        status='RESOLVED',
                        resolved_at=request_time + timedelta(minutes=random.randint(2, 10)),
                        context_data={
                            'screen': 'YouTube',
                            'action_attempted': random_subtask.target_action,
                            'session_id': session.id,
                        }
                    )
                    # auto_now_add 필드를 세션 기간 내로 수정
                    HelpRequest.objects.filter(pk=help_request.pk).update(
                        created_at=request_time
                    )

        self.stdout.write(self.style.SUCCESS(f'과거 세션 {len(sessions)}개 생성'))

        # 7. UserProgress 생성 (다양한 상태)
        # 어려운 단계 정의 (병목점수가 높을 단계)
        difficult_step_titles = ['검색어 입력', '검색창 탭하기', '전체 화면 보기', '볼륨 조절']

        for student in students:
            # 학생마다 다른 진행률 - 80% 완료율 목표로 높게 설정
            completion_rate = random.uniform(0.7, 1.0)  # 70~100%
            completed_count = int(len(subtasks_list) * completion_rate)

            for idx, subtask in enumerate(subtasks_list[:completed_count]):
                # 완료된 진행 기록
                started = now - timedelta(hours=random.randint(1, 48))

                # 평균 소요 시간 4~7분 사이로 설정
                # 어려운 단계는 평균 6~7분, 일반 단계는 4~5분
                is_difficult = subtask.title in difficult_step_titles
                if is_difficult:
                    # 어려운 단계 - 지체율을 높이기 위해 일부 학생은 평균의 2배 이상 소요
                    if random.random() < 0.35:  # 35%의 학생이 지체 (병목점수 연동)
                        duration = timedelta(minutes=random.randint(12, 18))  # 지체 학생
                    else:
                        duration = timedelta(minutes=random.randint(6, 8))  # 일반 학생
                else:
                    # 일반 단계 - 4~5분
                    if random.random() < 0.1:  # 10%의 학생만 지체
                        duration = timedelta(minutes=random.randint(9, 12))  # 지체 학생
                    else:
                        duration = timedelta(minutes=random.randint(4, 5))  # 일반 학생

                UserProgress.objects.get_or_create(
                    user=student,
                    subtask=subtask,
                    defaults={
                        'status': 'COMPLETED',
                        'started_at': started,
                        'completed_at': started + duration,
                        'attempts': random.randint(1, 4) if is_difficult else random.randint(1, 2),
                        'help_count': random.randint(1, 3) if is_difficult else 0,
                    }
                )

            # 현재 진행 중인 단계
            if completed_count < len(subtasks_list):
                current_subtask = subtasks_list[completed_count]
                UserProgress.objects.get_or_create(
                    user=student,
                    subtask=current_subtask,
                    defaults={
                        'status': 'IN_PROGRESS',
                        'started_at': now - timedelta(minutes=random.randint(3, 10)),
                        'attempts': random.randint(1, 5),
                    }
                )

        self.stdout.write(self.style.SUCCESS('학생 진행 상태 생성'))

        # 8. 도움 요청 생성 (어려운 단계에 더 많이 집중)
        # 병목점수가 높은 단계에 도움 요청이 더 많이 가도록 가중치 부여
        difficult_subtasks = [s for s in subtasks_list if s.title in difficult_step_titles]
        normal_subtasks = [s for s in subtasks_list if s.title not in difficult_step_titles]

        help_requests_created = 0

        # 어려운 단계에 더 많은 도움 요청 생성 (모든 학생 대상)
        for student in students:
            # 어려운 단계: 각 학생이 2~3개의 어려운 단계에서 도움 요청
            for subtask in random.sample(difficult_subtasks, k=min(random.randint(2, 3), len(difficult_subtasks))):
                request_time = now - timedelta(hours=random.randint(1, 72))
                is_resolved = random.random() > 0.2  # 80% 해결됨

                help_request, created = HelpRequest.objects.get_or_create(
                    user=student,
                    subtask=subtask,
                    request_type='MANUAL',
                    created_at__date=request_time.date(),
                    defaults={
                        'request_type': random.choice(['MANUAL', 'AUTO']),
                        'status': 'RESOLVED' if is_resolved else 'PENDING',
                        'resolved_at': request_time + timedelta(minutes=random.randint(2, 10)) if is_resolved else None,
                        'context_data': {
                            'screen': 'YouTube',
                            'action_attempted': subtask.target_action,
                        }
                    }
                )
                if created:
                    help_requests_created += 1

            # 일반 단계: 20%의 학생만 도움 요청
            if random.random() < 0.2 and normal_subtasks:
                subtask = random.choice(normal_subtasks)
                request_time = now - timedelta(hours=random.randint(1, 72))

                help_request, created = HelpRequest.objects.get_or_create(
                    user=student,
                    subtask=subtask,
                    request_type='MANUAL',
                    created_at__date=request_time.date(),
                    defaults={
                        'request_type': random.choice(['MANUAL', 'AUTO']),
                        'status': 'RESOLVED',
                        'resolved_at': request_time + timedelta(minutes=random.randint(2, 5)),
                        'context_data': {
                            'screen': 'YouTube',
                            'action_attempted': subtask.target_action,
                        }
                    }
                )
                if created:
                    help_requests_created += 1

        self.stdout.write(self.style.SUCCESS(f'도움 요청 {help_requests_created}개 생성'))

        # 완료 메시지
        self.stdout.write('')
        self.stdout.write(self.style.SUCCESS('=' * 50))
        self.stdout.write(self.style.SUCCESS('샘플 데이터 생성 완료!'))
        self.stdout.write(self.style.SUCCESS('=' * 50))
        self.stdout.write('')
        self.stdout.write('로그인 정보:')
        self.stdout.write(f'  - 강사: instructor@test.com / test1234')
        self.stdout.write(f'  - 학생: student1@test.com ~ student10@test.com / test1234')
        self.stdout.write('')
        self.stdout.write(f'생성된 데이터:')
        self.stdout.write(f'  - 강의: {lecture.title}')
        self.stdout.write(f'  - 태스크: {len(tasks_data)}개')
        self.stdout.write(f'  - 서브태스크: {len(subtasks_list)}개')
        self.stdout.write(f'  - 수강생: {len(students)}명')
        self.stdout.write(f'  - 과거 세션: {len(sessions)}개')
        self.stdout.write(f'  - 도움 요청: {help_requests_created}개')
