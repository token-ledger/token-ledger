# ==========================================
# 1단계: 빌드 환경 (Java 25 규격 적용)
# ==========================================
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build

COPY . .
RUN chmod +x ./gradlew

# sample-app만 격리해서 빌드 (팀의 멀티 모듈 구조 존중)
RUN ./gradlew :token-ledger-sample-app:bootJar -x test

# ==========================================
# 2단계: 실행 환경 (Java 25 규격 적용)
# ==========================================
FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=builder /build/token-ledger-sample-app/build/libs/*SNAPSHOT.jar ./app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]