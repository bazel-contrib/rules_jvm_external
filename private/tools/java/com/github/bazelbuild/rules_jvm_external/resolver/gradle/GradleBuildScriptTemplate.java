// Copyright 2024 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.Exclusion;
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
            List<GradleDependency> dependencies,
            List<Exclusion> globalExclusions
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
            return map;
        }).collect(Collectors.toList()));

        contextMap.put("globalExclusions", globalExclusions.stream().map(exclusion -> {
            Map<String, Object> map = new HashMap<>();
            map.put("group", exclusion.group);
            map.put("module", exclusion.module);
            return map;
        }).collect(Collectors.toList()));

        // Render the output
        String output = template.apply(Context.newContext(contextMap)).trim();

        // Write to output file
        Files.writeString(outputPath, output);

    }
}
