@echo off
chcp 65001 >nul
echo [1/3] 기존 컨테이너를 안전하게 제거합니다...
:: sample-app 폴더로 이동해서 down 실행
cd token-ledger-sample-app
docker-compose down

echo [2/3] Spring Boot 빌드를 시작합니다...
:: 다시 루트로 나와서 빌드 실행 (gradlew는 루트에 있으니까요)
cd ..
call gradlew.bat :token-ledger-sample-app:bootJar -x test

echo [3/3] 최신 설정으로 컨테이너를 빌드하고 실행합니다...
:: 다시 폴더로 들어가서 up 실행
cd token-ledger-sample-app
docker-compose up --build -d

echo ✅ Token Ledger 로컬 환경이 완벽하게 업데이트되었습니다!
pause