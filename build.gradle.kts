plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "conectaseguros.co"
version = "0.0.1-SNAPSHOT"

springBoot {
    mainClass.set("conectaseguros.co.discovery_server.DiscoveryServerApplication")
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2025.0.0"

dependencies {
	// Spring Boot dependencies
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-security")

	// Annotations and utilities
	implementation("org.projectlombok:lombok")

    // Dotenv support
    implementation("io.github.cdimascio:dotenv-java")

    // Caffeine for caching
    implementation("com.github.ben-manes.caffeine:caffeine")

	// Development dependencies
	"developmentOnly"("org.springframework.boot:spring-boot-devtools")

	// Testing dependencies
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
    dependencies {
        dependency("com.github.ben-manes.caffeine:caffeine:3.2.2")
        dependency("io.github.cdimascio:dotenv-java:3.2.0")
    }
}

tasks.withType<Test> {
	useJUnitPlatform()
}
