#!/bin/bash

# 1. 기존 컨테이너 중지 및 제거
echo "[1/3] 기존 컨테이너를 안전하게 제거합니다..."
docker-compose down

# 2. 스프링 부트 빌드
echo "[2/3] Spring Boot 빌드를 시작합니다..."
./gradlew clean :token-ledger-sample-app:bootJar -x test

# 3. 도커 이미지 빌드 및 실행
echo "[3/3] 최신 설정으로 컨테이너를 빌드하고 실행합니다..."
docker-compose up --build -d

echo "✅ Token Ledger 로컬 환경이 완벽하게 업데이트되었습니다!"