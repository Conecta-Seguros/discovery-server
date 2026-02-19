plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	jacoco
}

group = "conectaseguros.co"
version = "0.0.1-SNAPSHOT"

springBoot {
    mainClass.set("conectaseguros.co.discovery_server.DiscoveryServerApplication")
    buildInfo()
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springCloudVersion"] = "2025.1.1"

dependencies {
	// Spring Boot dependencies
	implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-server")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-security")

    // Dotenv support
    implementation("io.github.cdimascio:dotenv-java")

    // Cache abstraction (activates @EnableCaching + CaffeineCacheManager auto-config)
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Caffeine for caching
    implementation("com.github.ben-manes.caffeine:caffeine")

	// Annotations and utilities
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

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
        dependency("com.github.ben-manes.caffeine:caffeine:3.2.3")
        dependency("io.github.cdimascio:dotenv-java:3.2.0")
        dependency("com.thoughtworks.xstream:xstream:1.4.21")
    }
}

jacoco {
	toolVersion = "0.8.14"
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
		html.required = true
	}
}

val toolchainVersion = java.toolchain.languageVersion.get().asInt()
val jvmCompatArgs = buildList {
    add("-Xshare:off")
    if (toolchainVersion >= 23) {
        add("--sun-misc-unsafe-memory-access=allow")
    }
}

tasks.bootRun {
    jvmArgs(jvmCompatArgs)
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs(jvmCompatArgs)
}
