import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Calculations and parsing with no Android in them: statistics, budgets, goals, insights,
// filters, receipts, CSV and formatting. Its tests run on the plain JVM.
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
    api(project(":core:model"))

    testImplementation(libs.junit)
}
