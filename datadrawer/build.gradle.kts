plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
}

android {
    namespace = "io.github.al3j4ndrolm.datadrawer"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // KSerializer and Flow appear in the public API.
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.startup)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "datadrawer"
            version = project.version.toString()

            afterEvaluate {
                from(components["release"])
            }
        }
    }

    repositories {
        // Private GitHub Packages registry. Credentials come from the publish workflow.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/al3j4ndrolm/DataDrawer")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
