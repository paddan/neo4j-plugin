plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "com.lindefors.neo4j"
version = "1.0.16"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
            untilBuild = provider { null }
        }
        changeNotes = providers.provider {
            val changelog = rootProject.file("CHANGELOG.md").readText()
            // Extract the first version block (from the first ## up to the next ##)
            val rest = changelog.substringAfter("## ")
            val block = if ("\n## " in rest) rest.substringBefore("\n## ") else rest
            val (header, body) = block.split("\n", limit = 2).let {
                it[0].trim() to it.getOrElse(1) { "" }.trim()
            }
            val version = header.substringBefore("]").trimStart('[')
            val html = buildString {
                append("<h3>$version</h3>")
                var inList = false
                for (line in body.lines()) {
                    when {
                        line.startsWith("### ") -> {
                            if (inList) { append("</ul>"); inList = false }
                            append("<h4>${line.removePrefix("### ")}</h4>")
                        }
                        line.startsWith("- ") -> {
                            if (!inList) { append("<ul>"); inList = true }
                            append("<li>${line.removePrefix("- ")}</li>")
                        }
                    }
                }
                if (inList) append("</ul>")
            }
            html
        }
    }

    publishing {
        token = providers.gradleProperty("intellijPlatform.publishingToken")
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.1")
        bundledPlugin("com.intellij.java")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("junit:junit:4.13.2")
    testImplementation("org.junit.vintage:junit-vintage-engine")
}

tasks.test {
    useJUnitPlatform()
}
