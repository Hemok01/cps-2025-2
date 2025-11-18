#!/usr/bin/env python
"""
테스트 사용자 생성 스크립트
"""
import os
import django

# Django 설정
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
django.setup()

from apps.accounts.models import User


def create_test_users():
    """테스트 사용자들 생성"""

    # 1. 강사 계정
    instructor_email = 'instructor@test.com'
    if not User.objects.filter(email=instructor_email).exists():
        instructor = User.objects.create_user(
            email=instructor_email,
            password='TestInstructor123!@#',
            name='테스트 강사',
            role='INSTRUCTOR',
            is_staff=True
        )
        print(f'✅ 강사 계정 생성됨: {instructor.email}')
    else:
        print(f'ℹ️  강사 계정 이미 존재: {instructor_email}')

    # 2. 학생 계정
    student_email = 'student@test.com'
    if not User.objects.filter(email=student_email).exists():
        student = User.objects.create_user(
            email=student_email,
            password='TestStudent123!@#',
            name='테스트 학생',
            role='STUDENT',
            age=65,
            digital_level='BEGINNER'
        )
        print(f'✅ 학생 계정 생성됨: {student.email}')
    else:
        print(f'ℹ️  학생 계정 이미 존재: {student_email}')

    print('\n🎉 테스트 계정 생성 완료!')
    print('\n📋 로그인 정보:')
    print('강사 - Email: instructor@test.com, Password: TestInstructor123!@#')
    print('학생 - Email: student@test.com, Password: TestStudent123!@#')


if __name__ == '__main__':
    create_test_users()
