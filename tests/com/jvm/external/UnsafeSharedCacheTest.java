package com.jvm.external;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class UnsafeSharedCacheTest {

  private static String OS = System.getProperty("os.name").toLowerCase();

  @Test
  public void test_jarsOnClassPath_areInTheSharedCache() throws IOException, URISyntaxException {
    // No support for symlinks on Windows. Let's skip that.
    if (OS.indexOf("win") != -1) {
      return;
    }
    boolean foundGuavaJar = false;
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    for (URL url : ((URLClassLoader) cl).getURLs()) {
      URI uri = url.toURI();
      String uriString = uri.toString();
      if (uriString.contains("repo1.maven.org") && uriString.contains("guava")) {
        foundGuavaJar = true;
        if (OS.indexOf("mac") >= 0) {
          // Mac.
          // The jar's path is automatically resolved to the actual location on macOS.
          assertThat(uriString, containsString("/tmp/custom_coursier_cache"));
        } else {
          // Linux.
          // Assert that the symlink points to the jar in the default Coursier shared cache location
          // https://github.com/coursier/coursier/blob/master/doc/docs/cache.md#location
          if (Files.isSymbolicLink(Paths.get(uri))) {
            // If it's a symlink, we'll need to resolve it twice: output base -> shared cache.
            Path symlinkDest = Files.readSymbolicLink(Files.readSymbolicLink(Paths.get(uri)));
            assertThat(symlinkDest.toString(), containsString("/tmp/custom_coursier_cache"));
          } else {
            assertThat(uriString, containsString("/tmp/custom_coursier_cache"));
          }
        }
      }
    }
    assertTrue(foundGuavaJar);
  }
}
