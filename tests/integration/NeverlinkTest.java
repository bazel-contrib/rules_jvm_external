package com.jvm.external;


import org.junit.Test;
import java.net.URL;
import java.net.URLClassLoader;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

public class NeverlinkTest {

  @Test
  public void test_neverlinkArtifacts_notOnRuntimeClassPath() {
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    for (URL url : ((URLClassLoader) cl).getURLs()){
      assertThat(url.getFile().toString(), not(containsString("com/squareup/javapoet")));
    }
  }

}
