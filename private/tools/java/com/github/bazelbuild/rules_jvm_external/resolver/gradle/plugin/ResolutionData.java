package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import java.io.File;
import java.util.Objects;
import org.gradle.api.artifacts.result.ResolvedComponentResult;

class ResolutionData {
  private final ResolvedComponentResult result;
  private final File file;

  public ResolutionData(ResolvedComponentResult result, File file) {
    this.result = result;
    this.file = file;
  }

  public ResolvedComponentResult getResult() {
    return result;
  }

  public File getFile() {
    return file;
  }

  @Override
  public String toString() {
    return "ResolutionData{"
        + "result="
        + result.getId()
        + ", file="
        + (file == null ? "null" : file.getName())
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ResolutionData)) {
      return false;
    }
    ResolutionData that = (ResolutionData) o;
    return Objects.equals(result, that.result) && Objects.equals(file, that.file);
  }

  @Override
  public int hashCode() {
    return Objects.hash(result, file);
  }
}
