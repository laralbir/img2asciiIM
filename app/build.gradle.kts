import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.laralbir.img2asciiim"
  compileSdk {
    version = release(37) {
      minorApiLevel = 0
    }
  }
  buildToolsVersion = "37.0.0"
  defaultConfig {
    applicationId = "com.laralbir.img2asciiim"
    minSdk = 26
    targetSdk = 37
    versionCode = 2
    versionName = "1.0.1"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
      val keystorePath = System.getenv("KEYSTORE_PATH")
      val keyAliasVal = System.getenv("KEY_ALIAS")
      val keyPasswordVal = System.getenv("KEY_PASSWORD")
      val storePasswordVal = System.getenv("KEYSTORE_PASSWORD")

      if (!keystoreBase64.isNullOrBlank()) {
        val keystoreFile = layout.buildDirectory.file("keystore/release.keystore").get().asFile
        keystoreFile.parentFile?.mkdirs()
        keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
        storeFile = keystoreFile
        storePassword = storePasswordVal
        keyAlias = keyAliasVal
        keyPassword = keyPasswordVal
      } else if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
        storeFile = file(keystorePath)
        storePassword = storePasswordVal
        keyAlias = keyAliasVal
        keyPassword = keyPasswordVal
      } else {
        // Fallback to debug keystore for development / local assembleRelease builds
        val debugSigning = getByName("debug")
        storeFile = debugSigning.storeFile
        storePassword = debugSigning.storePassword
        keyAlias = debugSigning.keyAlias
        keyPassword = debugSigning.keyPassword
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  testImplementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.exifinterface)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: JUnit, Kotlin Test, Coroutines, Turbine, Robolectric + Compose UI
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.test.ext.junit)
  testImplementation(libs.androidx.compose.ui.test.junit4)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
