import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Clave de firma fija: variables de entorno (CI) o ~/.altgraph/keystore.properties (local).
// Sin ella se firma con la clave debug, que cambia en cada máquina/ejecución y rompe las actualizaciones OTA.
val localKeystore = Properties().apply {
    val f = File(System.getProperty("user.home"), ".altgraph/keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(env: String, prop: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: localKeystore.getProperty(prop)
val altgraphStoreFile = signingValue("ALTGRAPH_KEYSTORE_FILE", "storeFile")

android {
    namespace = "com.example.altgraph"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.altgraph"
        minSdk = 26
        targetSdk = 34
        versionCode = 206
        versionName = "1.1.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (altgraphStoreFile != null) {
            create("altgraph") {
                storeFile = file(altgraphStoreFile)
                storePassword = signingValue("ALTGRAPH_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("ALTGRAPH_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("ALTGRAPH_KEY_PASSWORD", "keyPassword")
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.findByName("altgraph") ?: signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("altgraph") ?: signingConfigs.getByName("debug")
        }
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Dependencias de Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // SDK oficial de Karoo Extension
    implementation("io.hammerhead:karoo-ext:1.1.9")
}