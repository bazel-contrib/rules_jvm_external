package com.github.bazelbuild.rules_jvm_external.resolver.lockfile;

import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.coursier.LockFileConverter;
import com.google.devtools.build.runfiles.Runfiles;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class LockFileConverterTest {

  @Test
  public void testConvertLockFile() throws IOException {
    final Path json =
        Paths.get(
            Runfiles.create()
                .rlocation(
                    "rules_jvm_external/tests/com/github/bazelbuild/rules_jvm_external/resolver/lockfile/unsorted.json"));

    final LockFileConverter lockFileConverter = new LockFileConverter(Set.of(), json);

    lockFileConverter.getDependencies();
    Map<String, Set<String>> exclusions = lockFileConverter.getExclusions();

    assertTrue(exclusions.containsKey("com.google.guava:guava"));
    assertTrue(
        exclusions
            .get("com.google.guava:guava")
            .contains("com.google.errorprone:error_prone_annotations"));
    assertTrue(exclusions.get("com.google.guava:guava").size() == 1);
  }
}
