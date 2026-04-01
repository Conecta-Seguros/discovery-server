# ── Build Args ──
ARG JAVA_VERSION=25
ARG APP_VERSION=1.0.0

# ============================================
# STAGE 1: Build (incremental, usar cache)
# ============================================
FROM gradle:jdk${JAVA_VERSION}-alpine AS builder

WORKDIR /build

# Copiar archivos de configuración primero (para cache)
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle.properties* ./
COPY gradle/ gradle/

# Descargar dependencias (sin código fuente para cache)
RUN gradle dependencies --no-daemon --stacktrace || true

# Copiar código fuente
COPY src/ src/

# Build con verificación
RUN gradle clean bootJar --no-daemon --stacktrace -x test && \
    test $(ls /build/build/libs/*.jar 2>/dev/null | wc -l) -eq 1

# ============================================
# STAGE 2: Layer Extraction
# ============================================
FROM eclipse-temurin:${JAVA_VERSION}-jdk-alpine AS extractor

WORKDIR /extract

COPY --from=builder /build/build/libs/*.jar application.jar

RUN java -Djarmode=layertools -jar application.jar extract --destination layers/ && \
    test -d layers/dependencies && \
    test -d layers/spring-boot-loader && \
    test -d layers/application

# ============================================
# STAGE 3: Runtime (Optimizado)
# ============================================
FROM eclipse-temurin:${JAVA_VERSION}-jre-alpine AS runtime

# ── Metadata ──
ARG APP_VERSION
LABEL maintainer="Caicedo Seguros <desarrollo@caicedoseguros.com>" \
      org.opencontainers.image.title="Discovery Server" \
      org.opencontainers.image.description="Netflix Eureka Discovery Server" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.vendor="Caicedo Seguros" \
      org.opencontainers.image.source="https://github.com/caicedo-seguros/discovery-server"

# ── System setup (optimizado) ──
RUN apk add --no-cache \
      wget \
      tzdata \
    && apk upgrade --no-cache \
    && cp /usr/share/zoneinfo/America/Bogota /etc/localtime \
    && echo "America/Bogota" > /etc/timezone \
    && apk del tzdata \
    && addgroup -S -g 1001 spring \
    && adduser -S -u 1001 -G spring -s /sbin/nologin spring \
    && mkdir -p /app \
    && chown -R spring:spring /app \
    && rm -rf /var/cache/apk/* \
    && /bin/sh -c 'echo "socket.so_rcvbuf_default=262144" >> /etc/sysctl.d/99-jvm.conf' \
    && /bin/sh -c 'echo "socket.sndbuf_default=262144" >> /etc/sysctl.d/99-jvm.conf'

WORKDIR /app

# ── Copy layers (orden optimizado para cache) ──
COPY --from=extractor --chown=spring:spring /extract/layers/dependencies/ ./
COPY --from=extractor --chown=spring:spring /extract/layers/spring-boot-loader/ ./
COPY --from=extractor --chown=spring:spring /extract/layers/snapshot-dependencies/ ./
COPY --from=extractor --chown=spring:spring /extract/layers/application/ ./

# ── Security ──
USER spring:spring

# ── Port ──
EXPOSE 8761

# ── Health check (optimizado) ──
HEALTHCHECK --interval=30s \
            --timeout=5s \
            --start-period=60s \
            --retries=3 \
    CMD wget -q --spider http://localhost:8761/actuator/health/liveness || exit 1

# ── Environment ──
ENV JAVA_OPTS="" \
    SPRING_MAIN_CLOUD_PLATFORM=kubernetes

# ── JVM Tuning ──
ENV JAVA_TOOL_OPTIONS="\
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=50.0 \
    -XX:InitialRAMPercentage=25.0 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=100 \
    -XX:+UseStringDeduplication \
    -XX:+ClassUnloadingWithConcurrentMark \
    -XX:+UnlockCommercialFeatures \
    -XX:+UseGCLogFileRotation \
    -XX:NumberOfGCLogFiles=5 \
    -XX:GCLogFileSize=10M \
    -Djava.security.egd=file:/dev/./urandom \
    -Djava.awt.headless=true \
    -Dfile.encoding=UTF-8 \
    -XX:+AlwaysPreTouch \
    -XX:+UseTransparentHugePages"

# ── Entrypoint ──
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]