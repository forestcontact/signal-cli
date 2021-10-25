plugins {
    java
    application
    eclipse
    `check-lib-versions`
    id("org.graalvm.buildtools.native") version "0.9.6"
}

version = "0.9.1"

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("org.asamk.signal.Main")
}

graalvmNative {
    binaries {
        this["main"].run {
            configurationFileDirectories.from(file("graalvm-config-dir"))
            buildArgs.add("--allow-incomplete-classpath")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("org.bouncycastle:bcprov-jdk15on:1.69")
    implementation("net.sourceforge.argparse4j:argparse4j:0.9.0")
    implementation("com.github.hypfvieh:dbus-java:3.3.0")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation(project(":lib"))
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}


tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to application.mainClass.get()
        )
    }
}

val assembleNativeImage by tasks.registering {
    dependsOn("assemble")

    var graalVMHome = ""
    doFirst {
        graalVMHome = System.getenv("GRAALVM_HOME")
            ?: throw GradleException("Required GRAALVM_HOME environment variable not set.")
    }

    doLast {
        val nativeBinaryOutputPath = "$buildDir/native-image"
        val nativeBinaryName = "signal-cli"

        mkdir(nativeBinaryOutputPath)

        exec {
            workingDir = File(".")
            commandLine(
                "$graalVMHome/bin/native-image",
                "-H:Path=$nativeBinaryOutputPath",
                "-H:Name=$nativeBinaryName",
                "-H:JNIConfigurationFiles=graalvm-config-dir/jni-config.json",
                "-H:DynamicProxyConfigurationFiles=graalvm-config-dir/proxy-config.json",
                "-H:ResourceConfigurationFiles=graalvm-config-dir/resource-config.json",
                "-H:ReflectionConfigurationFiles=graalvm-config-dir/reflect-config.json",
                "--no-fallback",
                "--allow-incomplete-classpath",
                "--report-unsupported-elements-at-runtime",
                "--enable-url-protocols=http,https",
                "--enable-https",
                "--enable-all-security-services",
                "-cp",
                sourceSets.main.get().runtimeClasspath.asPath,
                application.mainClass.get()
            )
        }
    }
}
