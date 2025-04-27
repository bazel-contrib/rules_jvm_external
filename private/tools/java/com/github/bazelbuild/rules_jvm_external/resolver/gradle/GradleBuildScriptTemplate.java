package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.GradleDependency;
import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Helper class to render a build.gradle.kts from the set of the dependencies we have
 * so that we can run the gradle resolver on it.
 */
public class GradleBuildScriptTemplate {

    private static final Handlebars handlebars = new Handlebars();

    static {
        // Register lowerCase helper to format dependency scopes
        handlebars.registerHelper("lowerCase", (context, options) -> {
            if (context == null) return "";
            String raw = context.toString().toLowerCase();
            if (raw.contains("_")) {
                String[] parts = raw.split("_");
                return parts[0] + parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
            }
            return raw;
        });
    }

    /**
     * Renders a build.gradle.kts from a template and a context.
     *
     * @param templatePath Path to build.gradle.kts.hbs template
     * @param outputPath   Path to write the rendered build.gradle.kts
     * @param repositories List of repository definitions
     * @param boms List of BOM platform dependencies
     * @param dependencies List of non-BOM dependencies
     * @throws IOException on file read/write error
     */
    public static void generateBuildGradleKts(
            Path templatePath,
            Path outputPath,
            List<Repository> repositories,
            List<GradleDependency> boms,
            List<GradleDependency> dependencies
    ) throws IOException {
        String templateContent = Files.readString(templatePath);

        // Compile the template
        Template template = handlebars.compileInline(templateContent);

        // Build the Handlebars context
        Map<String, Object> contextMap = new HashMap<>();

        contextMap.put("repositories", repositories.stream().map(repo -> {
            Map<String, Object> map = new HashMap<>();
            map.put("url", repo.getUrl());
            map.put("requiresAuth", repo.requiresAuth);
            if (repo.requiresAuth) {
                map.put("usernameProperty", repo.usernameProperty);
                map.put("passwordProperty", repo.passwordProperty);
            }
            return map;
        }).collect(Collectors.toList()));

        contextMap.put("boms", boms.stream().map(dep -> {
            Map<String, Object> map = new HashMap<>();
            map.put("group", dep.group);
            map.put("artifact", dep.artifact);
            map.put("version", dep.version);
            return map;
        }).collect(Collectors.toList()));

        contextMap.put("dependencies", dependencies.stream().map(dep -> {
            Map<String, Object> map = new HashMap<>();
            map.put("scope", dep.scope.name());  // e.g., "IMPLEMENTATION"
            map.put("group", dep.group);
            map.put("artifact", dep.artifact);
            map.put("version", dep.version);
            // You can add "exclusions" if you model it later
            return map;
        }).collect(Collectors.toList()));

        // Render the output
        String output = template.apply(Context.newContext(contextMap)).trim(); // Trim trailing newlines

        // Write to output file
        Files.writeString(outputPath, output);

    }
}
