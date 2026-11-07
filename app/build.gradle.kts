import java.io.InputStream
import java.security.KeyStore
import java.util.Base64
import java.util.Collections

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
    versionCode = 5
    versionName = "1.0.4"
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
      val keystorePath = System.getenv("KEYSTORE_PATH")
      val keyAliasVal = System.getenv("KEY_ALIAS")
      val keyPasswordVal = System.getenv("KEY_PASSWORD")
      val storePasswordVal = System.getenv("KEYSTORE_PASSWORD")

      val resolvedStoreFile = when {
        !keystoreBase64.isNullOrBlank() -> {
          val keystoreFile = layout.buildDirectory.file("keystore/release.keystore").get().asFile
          keystoreFile.parentFile?.mkdirs()
          val cleanedBase64 = keystoreBase64.replace("\\s+".toRegex(), "")
          val decodedBytes = try {
            Base64.getMimeDecoder().decode(cleanedBase64)
          } catch (_: Exception) {
            Base64.getDecoder().decode(cleanedBase64)
          }
          keystoreFile.writeBytes(decodedBytes)
          keystoreFile
        }
        !keystorePath.isNullOrBlank() && file(keystorePath).exists() -> file(keystorePath)
        else -> null
      }

      if (resolvedStoreFile != null) {
        var resolvedStorePassword = storePasswordVal?.trim() ?: storePasswordVal
        var resolvedKeyAlias = keyAliasVal?.trim() ?: keyAliasVal
        var resolvedKeyPassword = keyPasswordVal?.trim() ?: keyPasswordVal

        try {
          val storePassCandidates = listOfNotNull(
            storePasswordVal,
            storePasswordVal?.trim(),
            storePasswordVal?.removeSurrounding("\"")?.trim(),
            storePasswordVal?.removeSurrounding("'")?.trim(),
            keyPasswordVal,
            keyPasswordVal?.trim(),
            keyPasswordVal?.removeSurrounding("\"")?.trim(),
            keyPasswordVal?.removeSurrounding("'")?.trim()
          ).distinct()

          var loadedKs: KeyStore? = null
          for (sp in storePassCandidates) {
            try {
              val ks = KeyStore.getInstance(resolvedStoreFile, sp.toCharArray())
              loadedKs = ks
              resolvedStorePassword = sp
              break
            } catch (_: Exception) {
              for (type in listOf("PKCS12", "JKS")) {
                try {
                  val ks = KeyStore.getInstance(type)
                  resolvedStoreFile.inputStream().use { stream: InputStream ->
                    ks.load(stream, sp.toCharArray())
                  }
                  loadedKs = ks
                  resolvedStorePassword = sp
                  break
                } catch (_: Exception) {}
              }
              if (loadedKs != null) break
            }
          }

          if (loadedKs != null) {
            val aliases: List<String> = Collections.list(loadedKs.aliases())
            if (aliases.isNotEmpty()) {
              val matchedAlias = aliases.find { aliasName: String -> aliasName == resolvedKeyAlias }
                ?: aliases.find { aliasName: String -> aliasName.equals(resolvedKeyAlias, ignoreCase = true) }
                ?: if (aliases.size == 1) aliases.first() else resolvedKeyAlias

              if (matchedAlias != null) {
                resolvedKeyAlias = matchedAlias

                val keyPassCandidates = listOfNotNull(
                  keyPasswordVal,
                  keyPasswordVal?.trim(),
                  keyPasswordVal?.removeSurrounding("\"")?.trim(),
                  keyPasswordVal?.removeSurrounding("'")?.trim(),
                  resolvedStorePassword,
                  resolvedStorePassword?.trim(),
                  storePasswordVal,
                  storePasswordVal?.trim(),
                  storePasswordVal?.removeSurrounding("\"")?.trim(),
                  storePasswordVal?.removeSurrounding("'")?.trim(),
                  ""
                ).distinct()

                for (kp in keyPassCandidates) {
                  try {
                    val keyEntry = loadedKs.getKey(matchedAlias, kp.toCharArray())
                    if (keyEntry != null) {
                      resolvedKeyPassword = kp
                      break
                    }
                  } catch (_: Exception) {
                    // try next candidate
                  }
                }
              }
            }
          }
        } catch (_: Exception) {
          // If automatic inspection encounters any exception, proceed with defaults
        }

        storeFile = resolvedStoreFile
        storePassword = resolvedStorePassword
        keyAlias = resolvedKeyAlias
        keyPassword = resolvedKeyPassword
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
