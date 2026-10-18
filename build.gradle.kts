plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = property("group") as String
version = property("version") as String

val paperApiVersion = project.property("paperApiVersion") as String
val placeholderApiVersion = project.property("placeholderApiVersion") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val generateTemplates = tasks.register<Copy>("generateTemplates") {
    val properties = mapOf("version" to project.version)
    inputs.properties(properties)
    filteringCharset = "UTF-8"
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/templates"))
    expand(properties)
}

sourceSets.main {
    resources.srcDir(generateTemplates)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    shadowJar {
        archiveBaseName.set("DiamondSMP")
        archiveClassifier.set("")
        archiveVersion.set(project.version.toString())
    }

    jar {
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }
}
