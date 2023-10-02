plugins {
    id("org.jetbrains.dokka" ) version "1.9.10-SNAPSHOT"
    kotlin("js") version "1.9.0"
}


kotlin {
    js(IR) {
        browser()
        nodejs()
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}
dependencies {
    implementation(kotlin("stdlib"))
    implementation(npm("is-sorted", "1.0.5"))

    val reactVersion = "18.2.0-pre.597"
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react:$reactVersion")
    implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:$reactVersion")
}