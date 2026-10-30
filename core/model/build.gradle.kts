import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// The entities shared by every layer. Plain Kotlin, so nothing here can touch Android.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    // Only the annotations; the database itself lives in :core:data.
    api(libs.androidx.room.common)
}
