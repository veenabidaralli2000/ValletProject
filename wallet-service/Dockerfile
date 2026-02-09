# --- build stage ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# --- runtime stage ---
FROM eclipse-temurin:17-jre-jammy
RUN groupadd --system --gid 1001 wallet && \
    useradd  --system --uid 1001 --gid wallet wallet
WORKDIR /app
COPY --from=build /build/target/wallet-service.jar /app/wallet-service.jar
USER wallet
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/wallet-service.jar"]
