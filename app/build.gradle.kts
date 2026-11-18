plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.jhoel0521.studyplan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.jhoel0521.studyplan"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    sourceSets {
        // Los esquemas JSON de Room sirven a la prueba de migración (MigrationTestHelper)
        getByName("androidTest").assets.directories.add("$projectDir/schemas")
    }
    testOptions {
        // Las animaciones de las listas dejan filas duplicadas unos instantes y vuelven las pruebas de pantalla inestables
        animationsDisabled = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.biometric)
    implementation(libs.colorpickerview)
    implementation(libs.mpandroidchart)
    testImplementation(libs.junit)
    testImplementation(libs.arch.core.testing)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.ext.junit)
}

// room-testing 2.8.5 declara kotlinx-serialization 1.8.1, pero otra librería del proyecto lo baja a 1.7.3 y la
// prueba de migración falla con AbstractMethodError. Solo en las pruebas instrumentadas se fija la versión pedida.
configurations.matching { it.name.contains("AndroidTest", ignoreCase = true) }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
            useVersion("1.8.1")
        }
    }
}
