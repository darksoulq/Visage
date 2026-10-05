plugins {
    `java-library`
    signing
    id("net.thebugmc.gradle.sonatype-central-portal-publisher") version "1.2.4"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "io.github.darksoulq"
version = "1.0.1"

val targetJavaVersion = 25

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    paperweight.paperDevBundle("26.3.build.+")
    compileOnly("io.github.darksoulq:AbyssalLib:2.5.0-mc.26.3-alpha.3")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

centralPortal {
    name = "Visage"
    publishingType = net.thebugmc.gradle.sonatypepublisher.PublishingType.AUTOMATIC
    pom {
        name = "Visage"
        description = "Entity culling and display framework"
        url = "https://github.com/darksoulq/Visage"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
            }
        }
        developers {
            developer {
                id = "darksoulq"
                name = "darksoulq"
            }
        }
        scm {
            connection = "scm:git:git://github.com/darksoulq/Visage.git"
            developerConnection = "scm:git:ssh://github.com/darksoulq/Visage.git"
            url = "https://github.com/darksoulq/Visage"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("snapshot") {
            from(components["java"])
            artifactId = "Visage"

            pom {
                name.set("Visage")
                description.set("Entity culling and display framework")
                url.set("https://github.com/darksoulq/Visage")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("darksoulq")
                        name.set("darksoulq")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/darksoulq/Visage.git")
                    developerConnection.set("scm:git:ssh://github.com/darksoulq/Visage.git")
                    url.set("https://github.com/darksoulq/Visage")
                }
            }
        }
    }
    repositories {
        maven {
            name = "SonatypeSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            credentials {
                username = project.findProperty("centralPortal.username") as String?
                password = project.findProperty("centralPortal.password") as String?
            }
        }
    }
}

signing {
    val keyFile = rootProject.file("sonatype.asc")
    if (keyFile.exists()) {
        useInMemoryPgpKeys(
            project.providers.gradleProperty("signing.keyId").orNull,
            keyFile.readText(),
            project.providers.gradleProperty("signing.password").orNull
        )
    }
}