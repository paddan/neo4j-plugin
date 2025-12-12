plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.lindefors.neo4j"
version = "1.0.5"

repositories {
    mavenCentral()
}

intellij {
    version.set("2024.2")
    type.set("IU")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.patchPluginXml {
    sinceBuild.set("242")
    // Allow installation up to current IU-253 builds
    untilBuild.set("253.*")
}

tasks.test {
    useJUnitPlatform()
}
