import groovy.json.JsonBuilder
import java.io.File

repositories {
    maven {
        url = uri("https://repo1.maven.org/maven2")}
}

dependencies {
    implementation(platform("com.example:bom:0.1.0"))

    implementation("com.example:foo:0.0.1")

    implementation("com.example:bar:0.1.0")
}

tasks.register("dumpResolvedDependencies") {
    doLast {
        val output = mutableListOf<Map<String, Any?>>()

        configurations.filter { it.isCanBeResolved }.forEach { config ->
            val configEntry = mutableMapOf<String, Any?>(
                "configuration" to config.name,
                "artifacts" to mutableListOf<Map<String, String>>(),
                "unresolved" to mutableListOf<Map<String, String>>()
            )

            config.resolvedConfiguration.lenientConfiguration.artifacts.forEach { artifact ->
                (configEntry["artifacts"] as MutableList<Map<String, String>>).add(
                    mapOf(
                        "group" to artifact.moduleVersion.id.group,
                        "name" to artifact.name,
                        "version" to artifact.moduleVersion.id.version,
                        "filePath" to artifact.file.absolutePath
                    )
                )
            }

            config.resolvedConfiguration.lenientConfiguration.unresolvedModuleDependencies.forEach { unresolved ->
                (configEntry["unresolved"] as MutableList<Map<String, String>>).add(
                    mapOf(
                        "group" to unresolved.selector.group,
                        "name" to unresolved.selector.module,
                        "reason" to unresolved.problem.message
                    )
                )
            }

            output.add(configEntry)
        }

        val json = JsonBuilder(output).toPrettyString()
        val outputFilePath = project.findProperty("outputJsonFile") as String?
        val outputFile = if (outputFilePath != null) {
            File(outputFilePath)
        } else {
            File(buildDir, "dependency-resolution/resolved-dependencies.json")
        }
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json)

        println("Resolved dependencies dumped to ${outputFile.absolutePath}")
    }
}