plugins {
	java
	id("org.springframework.boot") version "3.3.13"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "capstone-project"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

allprojects {
	ext {
		set("lombokVersion", "1.18.26")
		set("mapstructVersion", "1.5.5.Final")
	}
}

dependencies {
	// Spring MVC
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-mail:3.4.1")
	implementation("org.springframework.boot:spring-boot-starter-quartz:3.4.3")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-json")

	// DB
	implementation("org.postgresql:postgresql")
	implementation("com.azure.spring:spring-cloud-azure-starter:5.22.0")

	// Swagger OpenAPI
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

	// Micrometer / tracing
	implementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
	implementation("io.micrometer:micrometer-tracing-bridge-otel:1.3.4")
	implementation("io.opentelemetry:opentelemetry-exporter-zipkin:1.42.1")
	implementation("io.micrometer:micrometer-tracing:1.3.4")

	// OAuth2
	implementation("org.springframework.security:spring-security-oauth2-client:6.4.2")

	// MapStruct
	implementation("org.mapstruct:mapstruct:${rootProject.ext.get("mapstructVersion")}")
	annotationProcessor("org.mapstruct:mapstruct-processor:${rootProject.ext.get("mapstructVersion")}")

	// Lombok
	compileOnly("org.projectlombok:lombok:${rootProject.ext.get("lombokVersion")}")
	annotationProcessor("org.projectlombok:lombok:${rootProject.ext.get("lombokVersion")}")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}


tasks.withType<Test> {
	useJUnitPlatform()
}
