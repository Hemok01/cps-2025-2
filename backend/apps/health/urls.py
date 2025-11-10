"""
Health Check URLs
"""
from django.urls import path
from . import views

urlpatterns = [
    path('', views.health_check, name='health-check'),
    path('detailed/', views.health_detailed, name='health-detailed'),
]
