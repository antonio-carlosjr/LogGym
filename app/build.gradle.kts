plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Credenciais só são lidas em invocações explícitas de Release; nunca entram no build de debug.
val releaseRequested = gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }
val releaseVariables = listOf("LOGGYM_RELEASE_STORE_FILE", "LOGGYM_RELEASE_STORE_PASSWORD",
    "LOGGYM_RELEASE_KEY_ALIAS", "LOGGYM_RELEASE_KEY_PASSWORD")
val releaseCredentials = if (releaseRequested) releaseVariables.associateWith { providers.environmentVariable(it).orNull }
    else emptyMap()
val validateReleaseCredentials = tasks.register("validateReleaseCredentials") {
    // O grafo de release não pode serializar senhas no cache de configuração.
    notCompatibleWithConfigurationCache("Release usa credenciais externas de assinatura.")
    doLast {
        val credentials = releaseVariables.associateWith { System.getenv(it) }
        if (!releaseRequested || credentials.values.any { it.isNullOrBlank() }) {
            throw GradleException("Assinatura indisponível. Configure as variáveis LOGGYM_RELEASE_* e execute assembleRelease --no-configuration-cache. Consulte docs/RELEASE.md.")
        }
        val keyFile = rootProject.file(credentials.getValue("LOGGYM_RELEASE_STORE_FILE")!!).canonicalFile
        if (!keyFile.isFile || keyFile.toPath().startsWith(rootProject.projectDir.canonicalFile.toPath())) {
            throw GradleException("O keystore de release deve existir fora do repositório.")
        }
    }
}

android {
    namespace = "com.loggym"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.loggym"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "com.loggym.HiltTestRunner"
    }

    signingConfigs {
        create("release") {
            if (releaseRequested) {
                releaseCredentials["LOGGYM_RELEASE_STORE_FILE"]?.takeIf { it.isNotBlank() }?.let { storeFile = rootProject.file(it) }
                storePassword = releaseCredentials["LOGGYM_RELEASE_STORE_PASSWORD"]
                keyAlias = releaseCredentials["LOGGYM_RELEASE_KEY_ALIAS"]
                keyPassword = releaseCredentials["LOGGYM_RELEASE_KEY_PASSWORD"]
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric precisa dos recursos Android nos testes da JVM.
        unitTests.isIncludeAndroidResources = true
        animationsDisabled = true
    }

    // Código de teste usado tanto na JVM (Robolectric) quanto no dispositivo.
    sourceSets {
        getByName("test").kotlin.srcDir("src/sharedTest/java")
        getByName("test").resources.srcDir("schemas")
        getByName("androidTest").kotlin.srcDir("src/sharedTest/java")
        getByName("androidTest").assets.srcDir("schemas")
    }
}

tasks.configureEach {
    if (name == "preReleaseBuild" || name == "validateSigningRelease") dependsOn(validateReleaseCredentials)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
