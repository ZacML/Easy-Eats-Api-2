# syntax=docker/dockerfile:1

# ---------- Estágio 1: build (Maven + JDK 25) ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# Copia só o necessário para resolver dependências: este layer fica em cache
# enquanto o pom.xml não mudar.
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Garante LF e permissão de execução (checkout no Windows pode trazer CRLF).
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw \
    && ./mvnw -B -q dependency:go-offline

COPY src src
# Testes ignorados no build: o contextLoads() precisa de um PostgreSQL ativo.
RUN ./mvnw -B -q -DskipTests package

# ---------- Estágio 2: runtime (só JRE, usuário não-root) ----------
FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
# A aplicação grava logs/spring-security.log relativo ao diretório atual.
RUN chown spring:spring /app
USER spring

COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
