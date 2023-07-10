plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "8.0.0"
    id("xyz.jpenilla.run-paper") version "2.0.1"
    id("io.papermc.paperweight.userdev") version "1.5.4"
}

group = "com.vanillage"
version = "1.11.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    paperweight.foliaDevBundle("1.20.1-R0.1-SNAPSHOT")
    compileOnly("com.mojang:datafixerupper:6.0.8")
    compileOnly("io.netty:netty-all:4.1.87.Final")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
}

tasks.compileJava {
    options.encoding = Charsets.UTF_8.name()
    options.release.set(17)
}

tasks.processResources {
    filter { line -> line.replace("\${version}", project.version.toString()) }
}

tasks.jar {
    enabled = false
}

tasks.assemble {
    dependsOn(tasks.reobfJar)
}
