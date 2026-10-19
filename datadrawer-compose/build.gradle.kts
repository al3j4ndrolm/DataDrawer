plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    `maven-publish`
}

android {
    namespace = "io.github.al3j4ndrolm.datadrawer.compose"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    api(project(":datadrawer"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = project.group.toString()
            artifactId = "datadrawer-compose"
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
