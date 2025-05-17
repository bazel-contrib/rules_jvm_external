import com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin.GradleDependencyModelPlugin

plugins.apply(GradleDependencyModelPlugin::class.java)

plugins {
   java
}

repositories {
    maven {
        url = uri("https://repo1.maven.org/maven2")}
}

// Support dependencies, BOMs and otherwise
dependencies {
    implementation("com.example:foo:0.0.1")

    implementation("com.example:bar:0.1.0")
}
