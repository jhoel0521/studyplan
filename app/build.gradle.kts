plugins {
    alias(libs.plugins.android.application)
}

// Versión y firma de publicación (RNF-15, RNF-16). Sin variables ni propiedades, el build local queda como siempre:
// versionCode 1, versionName "1.0" y release sin firmar. El workflow de release las pasa desde la etiqueta y los secretos.
val versionCodeProp = providers.gradleProperty("versionCode").map { it.toInt() }.getOrElse(1)
val versionNameProp = providers.gradleProperty("versionName").getOrElse("1.0")
val clavePath = providers.environmentVariable("STUDYPLAN_KEYSTORE_PATH").orNull
val claveStorePass = providers.environmentVariable("STUDYPLAN_KEYSTORE_PASSWORD").orNull
val claveAlias = providers.environmentVariable("STUDYPLAN_KEY_ALIAS").orNull
val claveKeyPass = providers.environmentVariable("STUDYPLAN_KEY_PASSWORD").orNull
val hayClaveDeFirma = listOf(clavePath, claveStorePass, claveAlias, claveKeyPass).all { !it.isNullOrBlank() }

android {
    namespace = "io.github.jhoel0521.studyplan"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "io.github.jhoel0521.studyplan"
        minSdk = 24
        targetSdk = 37
        versionCode = versionCodeProp
        versionName = versionNameProp

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    signingConfigs {
        if (hayClaveDeFirma) {
            create("release") {
                storeFile = file(clavePath!!)
                storePassword = claveStorePass
                keyAlias = claveAlias
                keyPassword = claveKeyPass
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            if (hayClaveDeFirma) {
                signingConfig = signingConfigs.getByName("release")
            }
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
