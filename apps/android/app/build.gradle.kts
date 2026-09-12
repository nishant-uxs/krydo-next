plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dev.krydo.mobile"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.krydo.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 22
        versionName = "0.22.0-onchain-tx"

        buildConfigField("String", "DEFAULT_API_BASE_URL", "\"https://krydo.onrender.com\"")
        buildConfigField("String", "WEB_APP_URL", "\"https://krydo-next.vercel.app\"")
        // Demo-only off-chain issue. Must also set ALLOW_OFFCHAIN_ISSUE=true on the API.
        val allowOffChain = (project.findProperty("allowOffChainIssue") as String?)?.trim()
            .equals("true", ignoreCase = true)
        buildConfigField("boolean", "ALLOW_OFFCHAIN_ISSUE", if (allowOffChain) "true" else "false")
        // Prefer local.properties reown.projectId=... ; fallback keeps Freighter WC usable for demos.
        val reownId = (project.findProperty("reown.projectId") as String?)?.trim().orEmpty()
            .ifBlank { "3a8170812b534d0ff9d794f19a901d64" }
        buildConfigField("String", "REOWN_PROJECT_ID", "\"$reownId\"")
        manifestPlaceholders["reownRedirect"] = "krydo://wc"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Camera (Scan preview + QR decode)
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")
    implementation("androidx.camera:camera-mlkit-vision:1.4.0")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Reown AppKit (EVM) + Sign (Stellar / Freighter WalletConnect)
    implementation(platform("com.reown:android-bom:1.4.11"))
    implementation("com.reown:android-core")
    implementation("com.reown:appkit")
    implementation("com.reown:sign")

    // Custom Tabs — Freighter Mobile connect via Krydo web WalletConnect flow
    implementation("androidx.browser:browser:1.8.0")

    // QR encode (share ZK proofs)
    implementation("com.google.zxing:core:3.5.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
