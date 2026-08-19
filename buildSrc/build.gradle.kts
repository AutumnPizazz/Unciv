plugins {
    `kotlin-dsl`
}

repositories {
    maven { url = uri("https://repo1.maven.org/maven2") }
}

dependencies {
    implementation(libs.gdx.tools) {
        exclude("com.badlogicgames.gdx", "gdx-backend-lwjgl")
    }
}
