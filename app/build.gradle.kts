import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("gg.jte.gradle") version "3.1.12"
    id("jacoco")
}

application {
    mainClass.set("hexlet.code.App")
}

group = "hexlet.code"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.javalin:javalin:6.1.3")
    implementation("io.javalin:javalin-rendering:6.1.3")
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("gg.jte:jte:3.1.12")
    implementation("com.h2database:h2:2.2.220")
    implementation("com.zaxxer:HikariCP:5.0.1")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jsoup:jsoup:1.17.2")
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.javalin:javalin-testtools:6.1.3")
    testImplementation("com.h2database:h2:2.2.220")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.register("stage") {
    dependsOn("clean", "shadowJar")
}

jte {
    generate()
    sourceDirectory.set(project.file("src/main/resources/templates").toPath())
    contentType.set(gg.jte.ContentType.Html)
}
tasks.shadowJar {
    mergeServiceFiles()
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "hexlet.code.App"
    }
}
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
}
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal()
            }
        }
    }
}
tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}
