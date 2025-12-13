import org.gradle.kotlin.dsl.implementation

plugins {
    java
    id("org.springframework.boot") version "3.3.13"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.microsoft.azure.azurewebapp") version "1.10.0"
    id("org.liquibase.gradle") version "2.2.2"
}

group = "capstone-project"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Configure jar file name for Railway deployment
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("app.jar")
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
    // SPRING BOOT STARTERS
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.boot:spring-boot-starter-mail:3.4.1")
    implementation("org.springframework.boot:spring-boot-starter-quartz:3.4.3")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf:3.5.5")
    
    // FILE UPLOAD
    implementation("commons-fileupload:commons-fileupload:1.5")

    // DATABASE
    implementation("org.postgresql:postgresql")
    implementation("com.azure.spring:spring-cloud-azure-starter:5.22.0")
    
    // LIQUIBASE - Database Migration
    implementation("org.liquibase:liquibase-core:4.29.2")

    // Redis & Cache
    implementation("org.springframework.boot:spring-boot-starter-data-redis:3.5.4")
    implementation("org.springframework.boot:spring-boot-starter-cache:3.5.4")
    implementation("org.apache.commons:commons-pool2:2.12.1")

    // SECURITY / AUTH
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    implementation("org.springframework.security:spring-security-oauth2-client:6.4.2")

    // OBSERVABILITY / MONITORING
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
    implementation("io.micrometer:micrometer-tracing-bridge-otel:1.3.4")
    implementation("io.micrometer:micrometer-tracing:1.3.4")
    implementation("io.opentelemetry:opentelemetry-exporter-zipkin:1.42.1")

    // API DOCUMENTATION
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // MAPPING / LOMBOK
    implementation("org.mapstruct:mapstruct:${rootProject.ext.get("mapstructVersion")}")
    annotationProcessor("org.mapstruct:mapstruct-processor:${rootProject.ext.get("mapstructVersion")}")

    compileOnly("org.projectlombok:lombok:${rootProject.ext.get("lombokVersion")}")
    annotationProcessor("org.projectlombok:lombok:${rootProject.ext.get("lombokVersion")}")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // JSON
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate6:2.17.0")

    // PDF / VIEW
    implementation("com.itextpdf:itextpdf:5.5.13.4")
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:html2pdf:4.0.5")
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.13.3")

    // CLOUD & INTEGRATION
    implementation("com.cloudinary:cloudinary-http44:1.39.0")
    implementation("com.google.firebase:firebase-admin:9.3.0")

    // AI & ML (REST API approach - no SDK needed)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // PAYMENT
    implementation("vn.payos:payos-java:2.0.1")
    implementation("com.stripe:stripe-java:29.5.0")

    // SOCKET / MESSAGING
    implementation("org.springframework:spring-websocket:6.2.3")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework:spring-messaging:6.1.6")

    // RATE LIMITING & SECURITY
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.6.0")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-jcache:7.6.0")
    implementation("javax.cache:cache-api:1.1.1")

    // DEVELOPMENT
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // TESTING
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("java.io.tmpdir", "${project.buildDir}/tmp")
    jvmArgs("-Xmx512m")
}

// Liquibase configuration
liquibase {
    // Main activity: apply all changelogs to main database
    activities.register("main") {
        arguments = mapOf(
            "changelogFile" to "db/changelog/db.changelog-master.xml",
            "url" to "jdbc:postgresql://localhost:5432/capstone-project",
            "username" to "postgres",
            "password" to "postgres",
            "driver" to "org.postgresql.Driver",
            "searchPath" to "src/main/resources"
        )
    }

    // Baseline: dùng cho generate baseline changelog từ schema hiện tại
    activities.register("generateBaseline") {
        arguments = mapOf(
            "changelogFile" to "db/changelog/baseline/db.changelog-baseline.xml",
            "url" to "jdbc:postgresql://localhost:5432/capstone-project",
            "username" to "postgres",
            "password" to "postgres",
            "driver" to "org.postgresql.Driver",
            "searchPath" to "src/main/resources"
        )
    }

    // Diff: generate file db/changelog/diff/db.changelog-diff.xml giữa reference DB và target DB
    activities.register("generateDiff") {
        arguments = mapOf(
            "changelogFile" to "db/changelog/diff/db.changelog-diff.xml",
            "url" to "jdbc:postgresql://localhost:5432/capstone-project",
            "username" to "postgres",
            "password" to "postgres",
            "referenceUrl" to "jdbc:postgresql://localhost:5432/capstone_reference",
            "referenceUsername" to "postgres",
            "referencePassword" to "postgres",
            "driver" to "org.postgresql.Driver",
            "searchPath" to "src/main/resources"
        )
    }

    // Test migration: apply master changelog lên test database
    activities.register("testMigration") {
        arguments = mapOf(
            "changelogFile" to "db/changelog/db.changelog-master.xml",
            "url" to "jdbc:postgresql://localhost:5432/capstone_test",
            "username" to "postgres",
            "password" to "postgres",
            "driver" to "org.postgresql.Driver",
            "searchPath" to "src/main/resources"
        )
    }

    runList = "main"
}

dependencies {
    liquibaseRuntime("org.liquibase:liquibase-core:4.29.2")
    liquibaseRuntime("org.postgresql:postgresql")
    liquibaseRuntime("info.picocli:picocli:4.7.5")
}
