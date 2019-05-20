package com.jvm.external;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import org.junit.Test;
import java.net.URL;
import java.net.URLClassLoader;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

public class GlobalArtifactExclusionsTest {

  @Test
  public void test_globallyExcludedArtifacts_notOnClassPah() throws IOException {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    ClassPath classPath = ClassPath.from(classLoader);
    ImmutableSet<ClassInfo> set = classPath.getTopLevelClasses();
    for (ClassInfo ci : set) {
      assertThat(ci.getName(), not(containsString("org.codehaus.mojo.animal_sniffer")));
      assertThat(ci.getName(), not(containsString("com.google.j2objc.annotations")));
    }
  }

}
