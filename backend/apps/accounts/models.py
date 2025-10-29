"""
User and Authentication Models
"""
from django.contrib.auth.models import AbstractBaseUser, BaseUserManager, PermissionsMixin
from django.db import models
from django.utils import timezone


class UserManager(BaseUserManager):
    """Custom user manager"""

    def create_user(self, email, password=None, **extra_fields):
        """Create and return a regular user"""
        if not email:
            raise ValueError('Users must have an email address')

        email = self.normalize_email(email)
        user = self.model(email=email, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, password=None, **extra_fields):
        """Create and return a superuser"""
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        extra_fields.setdefault('role', 'INSTRUCTOR')

        if extra_fields.get('is_staff') is not True:
            raise ValueError('Superuser must have is_staff=True.')
        if extra_fields.get('is_superuser') is not True:
            raise ValueError('Superuser must have is_superuser=True.')

        return self.create_user(email, password, **extra_fields)


class User(AbstractBaseUser, PermissionsMixin):
    """Custom User Model"""

    ROLE_CHOICES = [
        ('INSTRUCTOR', '강사'),
        ('STUDENT', '학생'),
    ]

    DIGITAL_LEVEL_CHOICES = [
        ('BEGINNER', '초급'),
        ('INTERMEDIATE', '중급'),
        ('ADVANCED', '고급'),
    ]

    email = models.EmailField(unique=True, max_length=255, verbose_name='이메일')
    phone = models.CharField(max_length=20, unique=True, null=True, blank=True, verbose_name='전화번호')
    name = models.CharField(max_length=100, verbose_name='이름')
    age = models.IntegerField(null=True, blank=True, verbose_name='나이')
    role = models.CharField(max_length=20, choices=ROLE_CHOICES, verbose_name='역할')
    digital_level = models.CharField(
        max_length=20,
        choices=DIGITAL_LEVEL_CHOICES,
        null=True,
        blank=True,
        verbose_name='디지털 수준'
    )

    is_active = models.BooleanField(default=True, verbose_name='활성 여부')
    is_staff = models.BooleanField(default=False, verbose_name='스태프 여부')
    created_at = models.DateTimeField(default=timezone.now, verbose_name='생성일시')
    updated_at = models.DateTimeField(auto_now=True, verbose_name='수정일시')
    last_login_at = models.DateTimeField(null=True, blank=True, verbose_name='마지막 로그인')

    objects = UserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['name', 'role']

    class Meta:
        db_table = 'users'
        verbose_name = '사용자'
        verbose_name_plural = '사용자'
        indexes = [
            models.Index(fields=['role']),
            models.Index(fields=['email']),
        ]

    def __str__(self):
        return f"{self.name} ({self.email})"

    def save(self, *args, **kwargs):
        """Override save to update last_login_at"""
        super().save(*args, **kwargs)
