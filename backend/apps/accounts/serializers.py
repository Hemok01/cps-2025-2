"""
User Serializers
"""
from rest_framework import serializers
from django.contrib.auth.password_validation import validate_password
from .models import User


class UserSerializer(serializers.ModelSerializer):
    """User serializer for general use"""

    class Meta:
        model = User
        fields = ['id', 'email', 'name', 'age', 'role', 'digital_level', 'phone', 'created_at']
        read_only_fields = ['id', 'created_at']


class UserRegistrationSerializer(serializers.ModelSerializer):
    """User registration serializer"""
    password = serializers.CharField(write_only=True, required=True, validators=[validate_password])
    password_confirm = serializers.CharField(write_only=True, required=True)

    class Meta:
        model = User
        fields = ['email', 'phone', 'password', 'password_confirm', 'name', 'age', 'role', 'digital_level']

    def validate(self, attrs):
        """Validate password match"""
        if attrs['password'] != attrs['password_confirm']:
            raise serializers.ValidationError({"password": "비밀번호가 일치하지 않습니다."})
        return attrs

    def create(self, validated_data):
        """Create user"""
        validated_data.pop('password_confirm')
        user = User.objects.create_user(**validated_data)
        return user


class UserDetailSerializer(serializers.ModelSerializer):
    """Detailed user serializer"""

    class Meta:
        model = User
        fields = ['id', 'email', 'phone', 'name', 'age', 'role', 'digital_level',
                  'is_active', 'created_at', 'updated_at', 'last_login_at']
        read_only_fields = ['id', 'created_at', 'updated_at', 'last_login_at']
