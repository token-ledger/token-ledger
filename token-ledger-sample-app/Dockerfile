# token-ledger-sample-app/Dockerfile

# ==========================================
# 실행 환경 (Java 25 규격 적용) - 빌드는 GitHub Actions가 담당
# ==========================================
FROM eclipse-temurin:25-jre
WORKDIR /app

# 깃허브 액션이 미리 빌드해둔 jar 파일을 컨테이너 내부로 복사
COPY token-ledger-sample-app/build/libs/*SNAPSHOT.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]