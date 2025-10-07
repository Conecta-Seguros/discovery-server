FROM ghcr.io/graalvm/graalvm-community:25-ol9

USER root

# Instalar dependencias
RUN microdnf update -y && \
    microdnf install -y git wget tar gzip unzip procps curl && \
    microdnf clean all

# Configurar JAVA_HOME con la ruta correcta
ENV JAVA_HOME=/opt/graalvm-community-java25 \
    PATH="${PATH}:/opt/graalvm-community-java25/bin"

# Verificar Java
RUN java -version

# Instalar Gradle 9.1.0
RUN wget -q https://services.gradle.org/distributions/gradle-9.1.0-bin.zip -P /tmp && \
    unzip -q -d /opt/gradle /tmp/gradle-9.1.0-bin.zip && \
    rm /tmp/gradle-9.1.0-bin.zip && \
    ln -s /opt/gradle/gradle-9.1.0/bin/gradle /usr/bin/gradle

# Configurar Gradle
ENV GRADLE_HOME=/opt/gradle/gradle-9.1.0 \
    PATH="${PATH}:/opt/gradle/gradle-9.1.0/bin"

# Crear usuario vscode
RUN useradd -m -s /bin/bash vscode && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Verificar instalaciones
RUN java -version && gradle --version

# Crear directorios
RUN mkdir -p /workspace && chown -R vscode:vscode /workspace && \
    mkdir -p /home/vscode/.gradle && chown -R vscode:vscode /home/vscode/.gradle

USER vscode
WORKDIR /workspace

EXPOSE 8761
CMD ["./gradlew", "bootRun"]