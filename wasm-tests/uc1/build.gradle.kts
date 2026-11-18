import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.pumpkinmc.plugin") version "0.1.2"
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        val wasmWasiMain by getting {
            kotlin.srcDir("build/generated/kotlin")
            kotlin.srcDir(rootProject.projectDir.parentFile.parentFile.resolve("pumpkin-compat/src/wasmWasiMain/kotlin"))
        }
    }
}

pumpkin {
    apiVersion.set(providers.environmentVariable("PUMPKIN_API_VERSION").orElse("0.1.2"))
    pluginClass.set("example.uc1.CoreUseCasePlugin")
}
