# 1. 자바 21 버전의 실행 환경을 가져옵니다.
FROM openjdk:21-jdk-slim

# 2. 컨테이너 내부 작업 폴더를 /app으로 설정합니다.
WORKDIR /app

# 3. 빌드 결과물인 jar 파일을 컨테이너 안으로 복사합니다.
# (Spring Boot 빌드 시 보통 build/libs 폴더에 생성됩니다.)
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

# 4. 앱이 사용할 포트를 명시합니다.
EXPOSE 8080

# 5. 앱을 실행하는 명령어를 입력합니다.
ENTRYPOINT ["java", "-jar", "app.jar"]