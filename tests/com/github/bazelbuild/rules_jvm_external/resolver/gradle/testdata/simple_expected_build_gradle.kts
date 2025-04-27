import groovy.json.JsonBuilder
import java.io.File

repositories {
    maven {
        url = uri("https://repo1.maven.org/maven2")}
}

dependencies {
    implementation("com.example:foo:0.0.1")

    implementation("com.example:bar:0.1.0")
}

abstract class ResolveDependenciesTask : DefaultTask() {

    data class DependencyInfo(
        val group: String,
        val name: String,
        val version: String,
        val requestedVersion: String?,
        val conflict: Boolean,
        val children: List<DependencyInfo> = emptyList()
    )

    @TaskAction
    fun resolveAndDump() {
        val configurationsToCheck = listOf(
            "compileClasspath",
            "runtimeClasspath",
            "testCompileClasspath",
            "testRuntimeClasspath"
        )

        val visited = mutableSetOf<ComponentIdentifier>()  // <<<<<<<< move here

        val allDependencies = mutableListOf<Map<String, Any?>>()

        configurationsToCheck.forEach { configName ->
            val configuration = project.configurations.findByName(configName) ?: return@forEach
            if (!configuration.isCanBeResolved) return@forEach

            val root = configuration.incoming.resolutionResult.root
            root.dependencies.forEach { dep ->
                visit(dep, visited)?.let { allDependencies.add(it.toMap()) }
            }
        }

        val outputPath = project.findProperty("outputJsonFile") as? String
        val outputFile = if (outputPath != null) {
            project.file(outputPath)
        } else {
            project.layout.buildDirectory.file("dependency-dump.json").get().asFile
        }

        outputFile.parentFile.mkdirs()
        outputFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(allDependencies)))

        println("Dependency graph dumped to: ${outputFile.absolutePath}")
    }

    private fun visit(dep: DependencyResult, visited: MutableSet<ComponentIdentifier>): DependencyInfo? {
        if (dep is ResolvedDependencyResult) {
            val selectedId = dep.selected.id
            val requested = (dep.requested as? ModuleComponentSelector)
            val selected = (selectedId as? ModuleComponentIdentifier)

            if (requested == null || selected == null) {
                return null
            }

            if (!visited.add(selectedId)) {
                // Already visited, skip to prevent infinite recursion
                return null
            }

            val requestedVersion = requested.version
            val selectedVersion = selected.version

            val children = dep.selected.dependencies.mapNotNull { child ->
                visit(child, visited)
            }

            return DependencyInfo(
                group = selected.group,
                name = selected.module,
                version = selectedVersion,
                requestedVersion = requestedVersion,
                conflict = (requestedVersion != selectedVersion),
                children = children
            )
        }
        if (dep is UnresolvedDependencyResult) {
            println("Unresolved dependency: ${dep.attempted}")
        }
        return null
    }

    private fun DependencyInfo.toMap(): Map<String, Any?> {
        return mapOf(
            "group" to group,
            "name" to name,
            "version" to version,
            "requestedVersion" to requestedVersion,
            "conflict" to conflict,
            "children" to children.map { it.toMap() }
        )
    }
}

tasks.register<ResolveDependenciesTask>("resolveDependencies")