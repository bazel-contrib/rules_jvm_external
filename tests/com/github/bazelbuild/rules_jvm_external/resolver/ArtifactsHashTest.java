package com.github.bazelbuild.rules_jvm_external.resolver;

import static com.github.bazelbuild.rules_jvm_external.resolver.ArtifactsHash.calculateArtifactsHash;
import static org.junit.Assert.assertEquals;

import com.github.bazelbuild.rules_jvm_external.resolver.lockfile.V2LockFile;
import com.google.devtools.build.runfiles.Runfiles;
import com.google.gson.Gson;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.Test;

// The tests in this file must match the tests in `artifacts_hash_test.bzl`
public class ArtifactsHashTest {
  @Test
  public void emptyInfos() throws IOException {
    assertHashCodesMatch(
        "rules_jvm_external/tests/custom_maven_install/artifacts_hash_no_deps_install.json");
  }

  @Test
  public void withDeps() throws IOException {
    assertHashCodesMatch(
        "rules_jvm_external/tests/custom_maven_install/artifacts_hash_with_deps_install.json");
  }

  @Test
  public void withDepsFromManve() throws IOException {
    assertHashCodesMatch(
        "rules_jvm_external/tests/custom_maven_install/artifacts_hash_with_deps_from_maven_install.json");
  }

  private void assertHashCodesMatch(String path) throws IOException {
    String file = Runfiles.create().rlocation(path);
    String contents = Files.readString(Paths.get(file));

    V2LockFile lockFile = V2LockFile.create(contents);

    Map<?, ?> read = new Gson().fromJson(contents, Map.class);
    Number expected = (Number) read.get("__RESOLVED_ARTIFACTS_HASH");

    assertEquals(expected.intValue(), calculateArtifactsHash(lockFile.getDependencyInfos()));
  }
}
