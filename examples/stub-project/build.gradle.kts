plugins {
    java
}

dependencies {
    implementation(files("../../libs/pumpkin-stub.jar"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
