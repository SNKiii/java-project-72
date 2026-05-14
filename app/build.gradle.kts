import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("gg.jte.gradle") version "3.1.12"
    id("jacoco")
    id("org.sonarqube") version "7.3.0.8198"
    id("checkstyle")
}

sonarqube {
    properties {
        property("sonar.projectKey", "SNKiii_java-project-72")
        property("sonar.organization", "snkiii")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.token", System.getenv("SONAR_TOKEN"))

        property("sonar.sources", "src/main/java")
        property("sonar.tests", "src/test/java")

        property("sonar.java.binaries", "build/classes/java/main")
        property("sonar.java.test.binaries", "build/classes/java/test")

        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.java.checkstyle.reportPaths", "build/reports/checkstyle/main.xml,build/reports/checkstyle/test.xml")

        property("sonar.coverage.exclusions", "**/config/*,**/dto/*,**/exceptions/*,**/App.java")

        property("sonar.issue.ignore.multicriteria", "e1")
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "checkstyle:com.puppycrawl.tools.checkstyle.checks.whitespace.FileTabCharacterCheck")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/*.java")
    }
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
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("com.konghq:unirest-java:3.14.5")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.javalin:javalin-testtools:6.1.3")
    testImplementation("com.h2database:h2:2.2.220")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

checkstyle {
    toolVersion = "10.12.4"
    configFile = file("config/checkstyle/checkstyle.xml") // Убедитесь, что файл существует
    isIgnoreFailures = false
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    violationRules {
        rule {
            limit {
                minimum = "0.30".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.checkstyleMain {
    exclude("**/build/generated-sources/**")
    exclude("**/jte/**")
}

tasks.checkstyleTest {
    exclude("**/build/generated-sources/**")
    exclude("**/jte/**")
}

tasks.shadowJar {
    mergeServiceFiles()
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "hexlet.code.App"
    }
}

jte {
    generate()
    sourceDirectory.set(project.file("src/main/resources/templates").toPath())
    contentType.set(gg.jte.ContentType.Html)
}

tasks.register("stage") {
    dependsOn("clean", "shadowJar")
}