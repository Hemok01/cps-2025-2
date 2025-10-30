#!/bin/bash

# MobileGPT Backend Server Startup Script

echo "====================================="
echo "MobileGPT Backend Server"
echo "====================================="
echo ""

# Activate virtual environment
echo "Activating virtual environment..."
source venv/bin/activate

# Run server
echo "Starting Django development server..."
echo "Server will be available at: http://localhost:8000"
echo ""
echo "API Endpoints:"
echo "  - Activity Log: POST http://localhost:8000/api/logs/activity/"
echo "  - Session Join: POST http://localhost:8000/api/students/sessions/join/"
echo "  - Admin Panel: http://localhost:8000/admin/"
echo ""
echo "Test Credentials:"
echo "  - Instructor: admin@example.com / admin123"
echo "  - Student: student1@example.com / student123"
echo "  - Session Code: TEST001"
echo ""
echo "Press Ctrl+C to stop the server"
echo "====================================="
echo ""

python manage.py runserver
