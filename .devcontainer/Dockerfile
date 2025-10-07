FROM ghcr.io/graalvm/graalvm-community:25-ol9

USER root

# Instalar dependencias primero
RUN microdnf update -y && \
    microdnf install -y \
    git \
    wget \
    tar \
    gzip \
    unzip \
    procps \
    curl \
    findutils \
    which && \
    microdnf clean all

# Encontrar y configurar JAVA_HOME automáticamente
RUN export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java)))) && \
    echo "export JAVA_HOME=$JAVA_HOME" >> /etc/profile.d/java.sh && \
    echo "export PATH=\$PATH:\$JAVA_HOME/bin" >> /etc/profile.d/java.sh

# Configurar variables de entorno permanentemente
ENV JAVA_HOME=/usr/lib/graalvm/graalvm-community-java25 \
    PATH="${PATH}:/usr/lib/graalvm/graalvm-community-java25/bin"

# Instalar Gradle 9.1.0
RUN wget -q https://services.gradle.org/distributions/gradle-9.1.0-bin.zip -P /tmp && \
    unzip -q -d /opt/gradle /tmp/gradle-9.1.0-bin.zip && \
    rm /tmp/gradle-9.1.0-bin.zip && \
    ln -s /opt/gradle/gradle-9.1.0/bin/gradle /usr/bin/gradle

ENV GRADLE_HOME=/opt/gradle/gradle-9.1.0 \
    PATH="${PATH}:/opt/gradle/gradle-9.1.0/bin"

# Crear usuario vscode
RUN useradd -m -s /bin/bash vscode && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Verificar instalaciones (ahora sí debería funcionar)
RUN java -version && gradle --version

# Crear directorios con permisos correctos
RUN mkdir -p /workspace && chown -R vscode:vscode /workspace && \
    mkdir -p /home/vscode/.gradle && chown -R vscode:vscode /home/vscode/.gradle

# Cambiar a usuario vscode
USER vscode

WORKDIR /workspace

EXPOSE 8761

CMD ["./gradlew", "bootRun"]