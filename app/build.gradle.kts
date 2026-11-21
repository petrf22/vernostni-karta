import com.android.build.api.artifact.SingleArtifact
import java.io.ByteArrayOutputStream
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Version: the root VERSION file (X.Y.Z) is the single source of truth, CHANGELOG.md describes it.
// versionCode is derived from it, so a release only ever edits VERSION and CHANGELOG.md.
val appVersion = rootProject.file("VERSION").readText().trim()
val appVersionCode = run {
    val parts = appVersion.split(".").map { it.toIntOrNull() }
    require(parts.size == 3 && parts.all { it != null && it in 0..99 }) {
        "VERSION must be X.Y.Z with each part 0..99, found '$appVersion'"
    }
    val (major, minor, patch) = parts.map { it!! }
    major * 10000 + minor * 100 + patch
}

// Upload key from Gradle properties (~/.gradle/gradle.properties or -P), never stored in the repo.
// If any value is missing, no signingConfig is created and bundleRelease builds an unsigned AAB
// (CI, other machines). Anything that goes to Google Play is built with :app:publishableBundle,
// which fails without the key instead.
val releaseStoreFile = findProperty("VERNOSTNIKARTA_STORE_FILE") as String?
val releaseStorePassword = findProperty("VERNOSTNIKARTA_STORE_PASSWORD") as String?
val releaseKeyAlias = findProperty("VERNOSTNIKARTA_KEY_ALIAS") as String?
val releaseKeyPassword = findProperty("VERNOSTNIKARTA_KEY_PASSWORD") as String?
val hasReleaseSigning = releaseStoreFile != null && releaseStorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "io.github.petrf22.vernostnikarta"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.petrf22.vernostnikarta"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersion
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // No dependency metadata in the APK/AAB (not needed, and it is a fingerprint of the build).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // Every language must be complete.
        error += listOf("MissingTranslation", "ExtraTranslation", "MissingQuantity")
        abortOnError = true
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.datastore.preferences)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.zxing.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.reorderable)

    testImplementation(libs.junit)
}

/**
 * Privacy guard: the build fails if the final (merged) manifest asks for network access,
 * e.g. because a newly added library declares it.
 */
abstract class CheckNoNetworkPermissionTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun check() {
        val text = mergedManifest.get().asFile.readText()
        val forbidden = listOf(
            "android.permission.INTERNET",
            "android.permission.ACCESS_NETWORK_STATE",
            "android.permission.ACCESS_WIFI_STATE",
        )
        val found = forbidden.filter { text.contains("\"$it\"") }
        if (found.isNotEmpty()) {
            throw GradleException("The app must not have network access, but the merged manifest contains: $found")
        }
    }
}

androidComponents {
    onVariants { variant ->
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register<CheckNoNetworkPermissionTask>("check${name}NoNetworkPermission") {
            group = "verification"
            description = "Fails if the ${variant.name} manifest requests network access."
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
        }
        tasks.named("check") { dependsOn(task) }
    }
}

// ---- Publishing: a signed and VERIFIED bundle for Google Play (docs/PLAY_STORE.md). Unlike
// bundleRelease, publishableBundle fails without the upload key, before the slow R8 build. ----

val checkReleaseSigning = tasks.register("checkReleaseSigning") {
    group = "publishing"
    description = "Fails unless all VERNOSTNIKARTA_* signing properties are set and the keystore exists."
    val missing = listOfNotNull(
        "VERNOSTNIKARTA_STORE_FILE".takeIf { releaseStoreFile == null },
        "VERNOSTNIKARTA_STORE_PASSWORD".takeIf { releaseStorePassword == null },
        "VERNOSTNIKARTA_KEY_ALIAS".takeIf { releaseKeyAlias == null },
        "VERNOSTNIKARTA_KEY_PASSWORD".takeIf { releaseKeyPassword == null },
    )
    val storeFilePath = releaseStoreFile
    val storeFileExists = releaseStoreFile?.let { file(it).isFile } ?: false
    doLast {
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Missing signing properties in ~/.gradle/gradle.properties: ${missing.joinToString()} " +
                    "(see docs/PLAY_STORE.md, \"Upload klíč\")."
            )
        }
        if (!storeFileExists) {
            throw GradleException("VERNOSTNIKARTA_STORE_FILE ('$storeFilePath') is not an existing file.")
        }
    }
    // Cheap check; never "up to date", that could hide a keystore that has disappeared.
    outputs.upToDateWhen { false }
}

/**
 * Verifies the AAB signature. Bundles are jar-signed, so the tool is jarsigner (apksigner cannot
 * read an AAB). `-strict` is not usable with a self-signed upload key, so the output is checked
 * for "jar verified" instead.
 */
abstract class VerifyJarSignatureTask : DefaultTask() {
    @get:InputFile
    abstract val artifact: RegularFileProperty

    @get:Internal
    abstract val jarsigner: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            commandLine(
                jarsigner.get().asFile.absolutePath, "-verify", "-verbose:summary", "-certs",
                artifact.get().asFile.absolutePath,
            )
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        val text = output.toString()
        if (result.exitValue != 0 || !text.contains("jar verified")) {
            throw GradleException("${artifact.get().asFile.name} is not validly signed:\n$text")
        }
        logger.lifecycle("Signature verified: ${artifact.get().asFile.name}")
    }
}

// jarsigner from the same JDK toolchain the app is built with (jvmToolchain(17) above).
val jarsignerPath = project.extensions.getByType(JavaToolchainService::class.java).launcherFor {
    languageVersion.set(JavaLanguageVersion.of(17))
}.map { it.metadata.installationPath.file("bin/jarsigner") }

val verifyBundleSignature = tasks.register<VerifyJarSignatureTask>("verifyBundleSignature") {
    group = "publishing"
    description = "Verifies the signature of app/build/outputs/bundle/release/app-release.aab."
    artifact.set(layout.buildDirectory.file("outputs/bundle/release/app-release.aab"))
    jarsigner.set(jarsignerPath)
    dependsOn("bundleRelease")
}

// bundleRelease is registered by AGP later; afterEvaluate guarantees it exists here.
afterEvaluate {
    tasks.named("bundleRelease") { mustRunAfter(checkReleaseSigning) }
}

tasks.register("publishableBundle") {
    group = "publishing"
    description = "Signed and verified AAB for Google Play; unlike bundleRelease it fails without the key."
    dependsOn(checkReleaseSigning, "bundleRelease", verifyBundleSignature)
    doLast { logger.lifecycle("Version $appVersion, versionCode $appVersionCode") }
}
