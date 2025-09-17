# Discovery Server - Conecta Seguros

## Descripción

El Discovery Server es un componente crítico de la plataforma de seguros Conecta Seguros que implementa el patrón de descubrimiento de servicios usando Netflix Eureka. Permite que los microservicios se registren automáticamente y descubran otros servicios en el entorno, facilitando la comunicación entre ellos sin necesidad de conocer las direcciones IP o puertos específicos. Está construido utilizando Spring Cloud Netflix Eureka Server.

## Tecnologías

- Java 24
- Spring Boot 3.5.3
- Spring Cloud Netflix Eureka Server
- Gradle
- Spring Security

## Arquitectura

El Discovery Server implementa el patrón de descubrimiento de servicios que centraliza las siguientes responsabilidades:

1. **Registro de Servicios**: Los microservicios se registran automáticamente al iniciar
2. **Descubrimiento de Servicios**: Los microservicios pueden encontrar otros servicios mediante el nombre del servicio
3. **Balanceo de Carga**: Proporciona información sobre las instancias disponibles de cada servicio
4. **Verificación de Estado**: Monitorea la salud de los servicios registrados

### Servicios Registrados

El servidor Eureka gestiona el registro de los siguientes microservicios:

- `api-gateway` - Puerta de enlace para todas las solicitudes
- `clients-service` - Gestión de clientes
- `news-service` - Gestión de noticias
- `products-service` - Gestión de productos

## Configuración de Variables de Entorno

El servicio utiliza credenciales básicas para proteger el dashboard de Eureka. Las credenciales se configuran en el archivo `application.properties`:

```properties
spring.security.user.name=eureka
spring.security.user.password=password
```

## Configuración del Servidor

El servidor Eureka se configura para no registrarse a sí mismo ni buscar otros registros:

- Puerto: `8761`
- Hostname: `localhost`
- URL de registro: `http://localhost:8761/eureka/`

## Ejecución Local

### Prerrequisitos

1. Java 24
2. Docker y Docker Compose (para ejecutar las dependencias)

### Usando Docker Compose (Recomendado)

1. Asegúrate de tener Docker instalado

2. Desde el directorio raíz del proyecto completo:
   ```bash
   docker-compose up -d
   ```

### Ejecución Directa

1. Ejecuta la aplicación:
   ```bash
   ./gradlew bootRun
   ```

### Ejecución con Gradle Wrapper (Windows)

```bash
gradlew.bat bootRun
```

## Puertos

El Discovery Server se ejecuta por defecto en el puerto `8761`.

## Dashboard de Eureka

El dashboard de Eureka está disponible en:

```
http://localhost:8761
```

Las credenciales predeterminadas son:
- Usuario: `eureka`
- Contraseña: `password`

## Construcción del Proyecto

Para construir el proyecto:

```bash
./gradlew build
```

O en Windows:

```bash
gradlew.bat build
```

## Empaquetado

Para crear un JAR ejecutable:

```bash
./gradlew bootJar
```

O en Windows:

```bash
gradlew.bat bootJar
```

## Despliegue

El servicio está diseñado para ser desplegado en contenedores Docker. Para crear una imagen Docker:

1. Construye el proyecto:
   ```bash
   ./gradlew bootJar
   ```

2. Crea la imagen Docker (necesitarás un Dockerfile):
   ```bash
   docker build -t discovery-server .
   ```

3. Ejecuta el contenedor:
   ```bash
   docker run -p 8761:8761 discovery-server
   ```

## Monitoreo

El servicio incluye Actuator de Spring Boot para monitoreo:

- Health check: `/actuator/health`
- Métricas: `/actuator/metrics`
- Información: `/actuator/info`

## Contribución

1. Crea una rama para tu funcionalidad (`git checkout -b feature/nueva-funcionalidad`)
2. Realiza tus cambios
3. Ejecuta las pruebas (`./gradlew test`)
4. Confirma tus cambios (`git commit -m 'Añadir nueva funcionalidad'`)
5. Envía la rama (`git push origin feature/nueva-funcionalidad`)
6. Abre un Pull Request

## Licencia

Este proyecto es parte de la plataforma Conecta Seguros y está sujeto a las políticas internas de la empresa.