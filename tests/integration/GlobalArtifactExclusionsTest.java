package com.jvm.external;


import org.junit.Test;
import java.net.URL;
import java.net.URLClassLoader;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

public class GlobalArtifactExclusionsTest {

  @Test
  public void test_globallyExcludedArtifacts_notOnClassPath() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    for (URL url : ((URLClassLoader) cl).getURLs()){
      assertThat(url.getFile().toString(), not(containsString("org/codehaus/mojo/animal-sniffer-annotations")));
      assertThat(url.getFile().toString(), not(containsString("com/google/j2objc/j2objc-annotations")));
    }
  }

}
