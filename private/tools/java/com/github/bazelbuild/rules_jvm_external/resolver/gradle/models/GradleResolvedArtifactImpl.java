package com.github.bazelbuild.rules_jvm_external.resolver.gradle.models;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class GradleResolvedArtifactImpl implements Serializable, GradleResolvedArtifact{
    private String classifier;
    private String extension;
    private File file;
    private Map<String, String> variantAttributes;

    public GradleResolvedArtifactImpl() {
    }

    @Override
    public String getClassifier() {
        return this.classifier;
    }

    @Override
    public String getExtension() {
        return this.extension;
    }

    @Override
    public File getFile() {
        return null;
    }

    @Override
    public Map<String, String> getVariantAttributes() {
        return variantAttributes;
    }

    @Override
    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    @Override
    public void setExtension(String extension) {
        this.extension = extension;
    }

    @Override
    public void setFile(File file) {
        this.file = file;
    }

    @Override
    public void setVariantAttributes(Map<String, String> variantAttributes) {
        this.variantAttributes = variantAttributes;
    }
}
