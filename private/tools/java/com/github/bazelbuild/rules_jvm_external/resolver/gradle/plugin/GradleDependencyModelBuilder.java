package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import com.github.bazelbuild.rules_jvm_external.resolver.gradle.models.*;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyConstraint;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.*;
import org.gradle.tooling.provider.model.ToolingModelBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class GradleDependencyModelBuilder implements ToolingModelBuilder {
    private HashMap<String, GradleDependency.Scope> configurationScopes = new LinkedHashMap<>(Map.of(
            "implementation", GradleDependency.Scope.IMPLEMENTATION,
            "runtimeOnly", GradleDependency.Scope.RUNTIME_ONLY,
            "compileOnly", GradleDependency.Scope.COMPILE_ONLY,
            "testImplementation", GradleDependency.Scope.TEST_IMPLEMENTATION
            ));

    @Override
    public boolean canBuild(String modelName) {
        return modelName.equals(GradleDependencyModel.class.getName());
    }

    @Override
    public Object buildAll(String modelName, Project project) {
        GradleDependencyModel gradleDependencyModel = new GradleDependencyModelImpl();
        // Loop through all possible configurations
        // and collect all dependency information on resolution.
        for(Configuration cfg: project.getConfigurations()) {
            if(!cfg.isCanBeResolved()) continue;

            List<GradleDependency> declaredDeps = new ArrayList<>();
            for (Dependency dep : cfg.getAllDependencies()) {
                if (!(dep instanceof ModuleDependency)) continue;
                ModuleDependency modDep = (ModuleDependency) dep;

                GradleDependency.Scope scope = configurationScopes.get(cfg.getName());
                List<Exclusion> exclusions = modDep.getExcludeRules().stream()
                        .map(rule -> new Exclusion(rule.getGroup(), rule.getModule()))
                        .collect(Collectors.toList());

                declaredDeps.add(new GradleDependency(
                        scope,
                        modDep.getGroup(),
                        modDep.getName(),
                        modDep.getVersion(),
                        exclusions
                ));
            }
            gradleDependencyModel.getDeclaredDependencies().put(cfg.getName(), declaredDeps);

            List<GradleResolvedDependency> resolvedRoots = new ArrayList<>();
            ResolutionResult result = cfg.getIncoming().getResolutionResult();
            ResolvedComponentResult root = result.getRoot();

            for (DependencyResult dep : root.getDependencies()) {
                if (!(dep instanceof ResolvedDependencyResult)) continue;

                ResolvedDependencyResult rdep = (ResolvedDependencyResult) dep;
                ResolvedComponentResult selected = rdep.getSelected();
                GradleResolvedDependency info = walkResolvedComponent(selected);

                if (rdep.getRequested() instanceof ModuleComponentSelector) {
                    String requested = ((ModuleComponentSelector) rdep.getRequested()).getVersion();
                    info.setRequestedVersion(requested);
                    info.setConflict(!Objects.equals(info.getVersion(), requested));
                }

                resolvedRoots.add(info);
            }
            gradleDependencyModel.getResolvedDependencies().put(cfg.getName(), resolvedRoots);

            List<GradleDependency> boms = new ArrayList<>();

            for (DependencyConstraint constraint : cfg.getAllDependencyConstraints()) {
                if (constraint.getGroup() == null || constraint.getVersion() == null) continue;

                GradleDependency.Scope scope = configurationScopes.get(cfg.getName());
                boms.add(new GradleDependency(
                        scope,
                        constraint.getGroup(),
                        constraint.getName(),
                        constraint.getVersion(),
                        List.of()
                ));
            }
            gradleDependencyModel.getBoms().put(cfg.getName(), boms);
        }

        return gradleDependencyModel;
    }

    private GradleResolvedDependency walkResolvedComponent(ResolvedComponentResult component) {
        GradleResolvedDependency info = new GradleResolvedDependency();
        info.setGroup(component.getModuleVersion().getGroup());
        info.setName(component.getModuleVersion().getName());
        info.setVersion(component.getModuleVersion().getVersion());
        info.setRequestedVersion(info.getVersion()); // default to the requested version
        info.setConflict(false); // will be set later if there's a conflict

        List<GradleResolvedDependency> children = new ArrayList<>();

        for (DependencyResult dep : component.getDependencies()) {
            if (!(dep instanceof ResolvedDependencyResult)) continue;

            ResolvedDependencyResult resolvedDep = (ResolvedDependencyResult) dep;
            ResolvedComponentResult selected = resolvedDep.getSelected();

            GradleResolvedDependency child = walkResolvedComponent(selected);

            if (resolvedDep.getRequested() instanceof ModuleComponentSelector) {
                String requestedVersion = ((ModuleComponentSelector)
                        resolvedDep.getRequested()).getVersion();
                child.setRequestedVersion(requestedVersion);
                child.setConflict(!Objects.equals(child.getVersion(), requestedVersion));
            }

            children.add(child);
        }

        ComponentSelectionReason reason = component.getSelectionReason();
        List<String> bomDescriptions = reason.getDescriptions().stream()
                        .map(descriptor -> descriptor.getDescription().toLowerCase())
                                .filter(description -> description.contains("platform") || description.contains("constraint"))
                                        .collect(Collectors.toList());
        info.setFromBom(!bomDescriptions.isEmpty());

        info.setChildren(children);
        return info;
    }
}
