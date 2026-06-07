#!/bin/bash

# 한글 깨짐 방지: 인코딩을 명시적으로 UTF-8로 지정
export LANG=en_US.UTF-8

echo "[1/3] 기존 컨테이너를 안전하게 제거합니다..."
cd token-ledger-sample-app
docker-compose down
cd ..

echo "[2/3] Spring Boot 빌드를 시작합니다..."
./gradlew clean :token-ledger-sample-app:bootJar -x test

echo "[3/3] 최신 설정으로 컨테이너를 빌드하고 실행합니다..."
cd token-ledger-sample-app
docker-compose up --build -d

echo "✅ Token Ledger 로컬 환경이 완벽하게 업데이트되었습니다!"