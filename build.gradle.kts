plugins {
    id("java-library")
    id("me.champeau.jmh") version "0.6.8"
}

group = "fan.zhuyi.swisstable"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:23.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
    jmhImplementation("org.openjdk.jmh:jmh-core:1.36")
    jmhImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.36")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.36")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    jvmArgs("--add-modules", "jdk.incubator.vector")
}

tasks.withType<JavaCompile> {
    options.compilerArgs?.plusAssign("--enable-preview")
    options.compilerArgs?.plusAssign("--add-modules")
    options.compilerArgs?.plusAssign("jdk.incubator.vector")
}


