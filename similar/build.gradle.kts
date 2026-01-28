plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.vanniktech.maven)
}

android {
    namespace = "com.guiyec.similar"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 16

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// To publish: ./gradlew publishToMavenCentral
mavenPublishing {
    coordinates("com.guiyec.similar", "similar", "0.9.7")

    pom {
        name.set("Similar")
        description.set("Android networking library that allows having similar code in multiplatform apps.")
        inceptionYear.set("2020")
        url.set("https://github.com/guiyec/Similar-Android")
        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/licenses/mit-license.php")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("guiyec")
                name.set("Guillermo Cique")
                url.set("https://github.com/GuiyeC")
                email.set("guiyec@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/guiyec/Similar-Android.git")
            connection.set("scm:git@github.com:guiyec/Similar-Android.git")
            developerConnection.set("scm:git@github.com:guiyec/Similar-Android.git")
        }
    }

    publishToMavenCentral(automaticRelease = true)

    signAllPublications()
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    api(libs.gson)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}