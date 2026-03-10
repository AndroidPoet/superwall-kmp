import io.androidpoet.superwall.Configuration

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.nexus.plugin)
}

apply(from = "$rootDir/scripts/publish-module.gradle.kts")

mavenPublishing {
  val artifactId = "superwall"
  coordinates(
    Configuration.artifactGroup,
    artifactId,
    rootProject.extra.get("libVersion").toString(),
  )

  pom {
    name.set(artifactId)
    description.set(
      "Kotlin Multiplatform SDK for Superwall — remote paywall configuration, A/B testing, and subscription management",
    )
  }
}

kotlin {
  androidTarget { publishLibraryVariants("release") }
  jvm("desktop")
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  macosX64()
  macosArm64()

  @Suppress("OPT_IN_USAGE")
  applyHierarchyTemplate {
    common {
      group("jvm") {
        withAndroidTarget()
        withJvm()
      }
      group("skia") {
        withJvm()
        group("darwin") {
          group("apple") {
            group("ios") {
              withIosX64()
              withIosArm64()
              withIosSimulatorArm64()
            }
            group("macos") {
              withMacosX64()
              withMacosArm64()
            }
          }
        }
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compilerOptions.configure {
        freeCompilerArgs.add("-Xexpect-actual-classes")
      }
    }
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
        implementation(libs.kotlinx.datetime)
        implementation(libs.ktor.client.core)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.ktor.serialization.json)
        api(libs.koin.core)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.ktor.client.okhttp)
        implementation(libs.play.billing)
        implementation(libs.androidx.appcompat)
        implementation(libs.androidx.lifecycle.runtime)
        implementation(libs.androidx.webkit)
        implementation(libs.androidx.browser)
        implementation(libs.androidx.activity.compose)
      }
    }

    val iosMain by getting {
      dependencies {
        implementation(libs.ktor.client.darwin)
      }
    }
  }

  explicitApi()
}

android {
  compileSdk = Configuration.compileSdk
  namespace = "io.androidpoet.superwall"

  defaultConfig {
    minSdk = Configuration.minSdk
  }

  buildFeatures {
    buildConfig = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  packaging {
    resources {
      excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
  }

  lint {
    abortOnError = false
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs +=
      listOf(
        "-Xexplicit-api=strict",
      )
  }
}
