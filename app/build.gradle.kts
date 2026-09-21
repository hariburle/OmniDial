import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.io.File
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.telecomdialer.hdyzif"
    minSdk = 24
    targetSdk = 36
    versionCode = 18
    versionName = "1.5.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  // Auto-restore debug.keystore from debug.keystore.base64 if missing (e.g. fresh git clone in desktop Android Studio)
  val debugKeystoreFile = file("../debug.keystore")
  val debugKeystoreBase64 = file("../debug.keystore.base64")
  if (!debugKeystoreFile.exists() && debugKeystoreBase64.exists()) {
    try {
      val decoded = Base64.getDecoder().decode(debugKeystoreBase64.readText().trim())
      debugKeystoreFile.writeBytes(decoded)
    } catch (e: Exception) {
      logger.warn("Could not restore debug.keystore from base64: ${e.message}")
    }
  }

  signingConfigs {
    getByName("debug") {
      storeFile = file("../debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH")?.ifBlank { null } ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      isMinifyEnabled = false
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("debug")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
  packaging {
    jniLibs {
      useLegacyPackaging = true
    }
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "DebugProbesKt.bin"
      excludes += "META-INF/*.version"
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

val releaseSigningConfig = android.signingConfigs.getByName("release")
val configuredStoreFilePath = releaseSigningConfig.storeFile?.absolutePath ?: ""
val configuredStorePassword = releaseSigningConfig.storePassword ?: ""
val configuredKeyAlias = releaseSigningConfig.keyAlias ?: ""
val configuredKeyPassword = releaseSigningConfig.keyPassword ?: ""
val rootDirPath = rootDir.absolutePath

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
  inputs.property("storeFilePath", configuredStoreFilePath)
  inputs.property("storePassword", configuredStorePassword)
  inputs.property("keyAlias", configuredKeyAlias)
  inputs.property("keyPassword", configuredKeyPassword)
  inputs.property("rootDirPath", rootDirPath)

  doFirst {
    val storePath = inputs.properties["storeFilePath"] as String
    val storePass = inputs.properties["storePassword"] as String
    val alias = inputs.properties["keyAlias"] as String
    val keyPass = inputs.properties["keyPassword"] as String
    val rootDirAbsPath = inputs.properties["rootDirPath"] as String

    val missing = mutableListOf<String>()

    val storeFile = File(storePath)
    if (storePath.isBlank() || !storeFile.isFile) {
      missing.add("Release keystore file not found at: ${if (storePath.isBlank()) "unspecified" else storePath}")
    }
    if (storePass.isBlank()) {
      missing.add("Keystore password is missing (set STORE_PASSWORD environment variable)")
    }
    if (alias.isBlank()) {
      missing.add("Key alias is missing (set KEY_ALIAS environment variable, defaults to 'upload')")
    }
    if (keyPass.isBlank()) {
      missing.add("Key password is missing (set KEY_PASSWORD environment variable)")
    }

    if (missing.isNotEmpty()) {
      throw GradleException(
        """
        |
        |================================================================================
        |RELEASE BUILD SIGNING CONFIGURATION ERROR:
        |Release builds cannot use debug keys or proceed without valid release signing.
        |
        |The following required signing configuration items are missing:
        |${missing.joinToString("\n") { "  - $it" }}
        |
        |HOW TO FIX:
        |1. Place your release keystore at:
        |   $rootDirAbsPath/my-upload-key.jks
        |   (or set KEYSTORE_PATH to point to your keystore file).
        |2. Set the STORE_PASSWORD environment variable to your keystore password.
        |3. Set the KEY_PASSWORD environment variable to your key password.
        |4. (Optional) Set KEY_ALIAS if different from default ('upload').
        |================================================================================
        """.trimMargin()
      )
    }
  }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.libphonenumber)
  // implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  // implementation(libs.firebase.appcheck.recaptcha)
  // implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.logging.interceptor)
  // implementation(libs.moshi.kotlin)
  // implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  // implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
