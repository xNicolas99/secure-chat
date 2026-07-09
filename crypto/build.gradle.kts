plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    compileOnly("com.goterl:lazysodium-java:5.1.1")
    compileOnly("net.java.dev.jna:jna:5.13.0")

    testImplementation("com.goterl:lazysodium-java:5.1.1")
    testImplementation("net.java.dev.jna:jna:5.13.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
