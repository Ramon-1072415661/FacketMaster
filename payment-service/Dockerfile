# ── Build stage ────────────────────────────────────────────────
#
# Usa -pl / -am do Maven para resolver o parent POM corretamente
# sem precisar de install:install-file manual.
#
# Contexto do docker-compose: context: .  (raiz do monorepo)
# Portanto este Dockerfile recebe toda a estrutura do projeto.
#
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copiar todos os POMs antes do código-fonte para aproveitar o cache
COPY pom.xml ./pom.xml
COPY common/pom.xml ./common/pom.xml
COPY event-service/pom.xml ./event-service/pom.xml
COPY payment-service/pom.xml ./payment-service/pom.xml

# 2. Copiar código-fonte de todos os módulos
COPY common/src ./common/src
COPY event-service/src ./event-service/src
COPY payment-service/src ./payment-service/src

# 3. Compilar e instalar todos os módulos no repositório Maven local do contêiner
#    Isso garante que o parent POM e o módulo common estejam disponíveis para os serviços.
RUN mvn install -N -DskipTests -q
RUN mvn clean install -pl common -DskipTests -q
RUN mvn clean package -pl payment-service -am -DskipTests -q

# ── Runtime stage ───────────────────────────────────────────────
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Usuário sem privilégios de root (boa prática de segurança)
RUN addgroup --system appgroup && useradd --system -g appgroup appuser
USER appuser

# O jar fica em payment-service/target/ (não em /app/target/)
COPY --from=build /app/payment-service/target/payment-service-*.jar app.jar

EXPOSE 8082

# Health check: aguarda 60s — payment-service sobe depois do RabbitMQ e Redis
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]