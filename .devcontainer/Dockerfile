FROM ghcr.io/graalvm/graalvm-community:25-ol9

USER root

RUN microdnf update -y && \
    microdnf install -y git wget tar gzip unzip procps curl && \
    microdnf clean all

# Instalar Gradle 9.1.0
RUN wget https://services.gradle.org/distributions/gradle-9.1.0-bin.zip -P /tmp && \
    unzip -d /opt/gradle /tmp/gradle-9.1.0-bin.zip && \
    rm /tmp/gradle-9.1.0-bin.zip && \
    ln -s /opt/gradle/gradle-9.1.0/bin/gradle /usr/bin/gradle

# Crear usuario vscode
RUN useradd -m -s /bin/bash vscode && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Variables de entorno
ENV JAVA_HOME=/opt/graalvm \
    GRADLE_HOME=/opt/gradle/gradle-9.1.0 \
    PATH="${PATH}:/opt/graalvm/bin:/opt/gradle/gradle-9.1.0/bin"

# Verificar instalaciones
RUN java -version && gradle --version

# Crear directorios
RUN mkdir -p /workspace && chown -R vscode:vscode /workspace
RUN mkdir -p /home/vscode/.gradle && chown -R vscode:vscode /home/vscode/.gradle

USER vscode
WORKDIR /workspace

EXPOSE 8761
CMD ["./gradlew", "bootRun"]