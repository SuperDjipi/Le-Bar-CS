plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    // Le plugin 'application' est la clé pour rendre le serveur exécutable.
    application
}

group = "club.djipi.lebarcs.server"
version = "1.0.0"

application {
    // Indique à Gradle quelle est la fonction 'main' à exécuter.
    mainClass.set("club.djipi.lebarcs.server.ApplicationKt") // !! Point important, voir note ci-dessous

    // Permet de passer des arguments à la JVM pour le mode développement de Ktor
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    // Ktor Server
    implementation(libs.bundles.ktor.server)
    implementation(project(":shared"))

    // Logging
    implementation(libs.logback.classic)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.junit)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
