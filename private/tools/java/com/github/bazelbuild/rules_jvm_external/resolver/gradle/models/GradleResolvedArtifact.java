package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.io.File;
import java.util.Map;

public interface GradleResolvedArtifact {
    public String getClassifier();
    public String getExtension();
    public File getFile();
    public Map<String, String> getVariantAttributes();
    public void setClassifier(String classifier);
    public void setExtension(String extension);
    public void setFile(File file);
    public void setVariantAttributes(Map<String, String> variantAttributes);
}
