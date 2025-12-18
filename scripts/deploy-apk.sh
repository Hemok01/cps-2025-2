#!/bin/bash

# MobileGPT Student APK Build & Deploy Script
# This script builds the Android app and copies the APK to the backend static folder

set -e  # Exit on error

echo "======================================"
echo "MobileGPT Student APK Build & Deploy"
echo "======================================"
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
ANDROID_DIR="android-student"
BACKEND_STATIC_DIR="backend/static/downloads"
APK_NAME="mobilegpt-student.apk"

# Check if we're in the project root
if [ ! -d "$ANDROID_DIR" ] || [ ! -d "backend" ]; then
    echo -e "${RED}Error: This script must be run from the project root directory${NC}"
    exit 1
fi

# Ask for build type
echo "Select build type:"
echo "1) Debug (for testing)"
echo "2) Release (for production)"
read -p "Enter choice (1 or 2): " BUILD_TYPE

if [ "$BUILD_TYPE" == "1" ]; then
    BUILD_VARIANT="Debug"
    GRADLE_TASK="assembleDebug"
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
elif [ "$BUILD_TYPE" == "2" ]; then
    BUILD_VARIANT="Release"
    GRADLE_TASK="assembleRelease"
    APK_PATH="app/build/outputs/apk/release/app-release.apk"
else
    echo -e "${RED}Invalid choice. Exiting.${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Building $BUILD_VARIANT APK...${NC}"
echo ""

# Navigate to Android directory and build
cd "$ANDROID_DIR"

# Clean previous builds
echo "Cleaning previous builds..."
./gradlew clean

# Build APK
echo ""
echo "Building APK..."
./gradlew $GRADLE_TASK

# Check if build was successful
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}Error: APK file not found at $APK_PATH${NC}"
    cd ..
    exit 1
fi

# Go back to project root
cd ..

# Copy APK to backend static folder
echo ""
echo -e "${BLUE}Copying APK to backend static folder...${NC}"
cp "$ANDROID_DIR/$APK_PATH" "$BACKEND_STATIC_DIR/$APK_NAME"

# Get APK file size
APK_SIZE=$(du -h "$BACKEND_STATIC_DIR/$APK_NAME" | cut -f1)

echo ""
echo -e "${GREEN}======================================"
echo "✓ Build and Deploy Complete!"
echo "======================================"
echo ""
echo "APK Location: $BACKEND_STATIC_DIR/$APK_NAME"
echo "APK Size: $APK_SIZE"
echo ""
echo "Download URL (dev): http://localhost:8000/static/downloads/$APK_NAME"
echo "Download URL (prod): https://yourdomain.com/static/downloads/$APK_NAME"
echo ""
echo "Next steps:"
echo "1. Start backend server: cd backend && python manage.py runserver"
echo "2. Test APK download from: http://localhost:8000/static/downloads/$APK_NAME"
echo "3. Or visit join page: http://localhost:5173/join/ABC123"
echo -e "${NC}"
