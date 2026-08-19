import java.util.Properties

pluginManagement {
    repositories {
        mavenLocal() // To get the compiler plugin locally
        gradlePluginPortal() // So other plugins can be resolved
        // Maven Central 源站：GitHub Actions runner 对 repo.maven.apache.org 偶发 403
        maven { url = uri("https://repo1.maven.org/maven2") }
    }
}

include("desktop", "core", "tests", "server")

private fun getSdkPath(): String? {
    val localProperties = file("local.properties")
    return if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }

        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_HOME")
    } else {
        System.getenv("ANDROID_HOME")
    }
}
if (getSdkPath() != null) include("android")
