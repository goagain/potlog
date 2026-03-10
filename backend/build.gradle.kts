plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
    id("com.gradleup.shadow") version "8.3.4"
    application
}

group = "com.potlog"
version = "1.0.0"

application {
    mainClass.set("com.potlog.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Djava.net.preferIPv4Stack=true")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.7"
val kotlinVersion = "1.9.22"
val logbackVersion = "1.4.14"
val mongoDriverVersion = "4.11.1"

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    
    // MongoDB
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:$mongoDriverVersion")
    implementation("org.mongodb:bson-kotlinx:$mongoDriverVersion")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<JavaExec>("run") {
    jvmArgs("-Djava.net.preferIPv4Stack=true")
    environment("HOST", "127.0.0.1")
    environment("PORT", "8081")  // 与 vite proxy 一致
}

tasks.register("buildFatJar") {
    dependsOn(tasks.shadowJar)
    group = "build"
    description = "Builds a fat JAR (alias for shadowJar, for Dockerfile compatibility)"
}
