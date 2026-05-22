@echo off
echo [1/3] 기존 컨테이너를 안전하게 제거합니다...
call docker-compose down

echo [2/3] Spring Boot 빌드를 시작합니다... (jar 파일 생성)
call gradlew.bat :token-ledger-sample-app:bootJar -x test

echo [3/3] 최신 설정으로 컨테이너를 빌드하고 실행합니다...
docker-compose up --build -d

echo ✅ Token Ledger 로컬 환경이 완벽하게 업데이트되었습니다!
pause