import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.1"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.2.21"
}

group = "com"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("software.amazon.awssdk:s3:2.25.8")
	implementation("com.h2database:h2:2.2.220")

	// Web3j für Blockchain Integration
	implementation("org.web3j:core:4.12.0")
	implementation("org.web3j:crypto:4.12.0")

	// Scheduling Support
	implementation("org.springframework.boot:spring-boot-starter")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

fun loadDotEnv(filePath: String = ".env"): Map<String, String> {
	val envFile = file(filePath)
	if (!envFile.exists()) return emptyMap()

	return envFile.readLines()
		.asSequence()
		.map { it.trim() }
		.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
		.map {
			val index = it.indexOf('=')
			val key = it.substring(0, index).trim()
			var value = it.substring(index + 1).trim()
			if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith('\'') && value.endsWith('\''))) {
				value = value.substring(1, value.length - 1)
			}
			key to value
		}
		.toMap()
}

tasks.named<BootRun>("bootRun") {
	val dotEnv = loadDotEnv()
	environment.putAll(dotEnv.filterKeys { System.getenv(it) == null })
}
