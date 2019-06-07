package com.jvm.external;


import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

public class ArtifactExclusionsTest {

  @Test
  public void test_excludedArtifacts_notOnClassPath() throws IOException {
    ClassPath classPath = ClassPath.from(ClassLoader.getSystemClassLoader());
    for (ClassInfo ci : classPath.getTopLevelClasses()) {
      assertThat(ci.getName(), not(containsString("org.codehaus.mojo.animal_sniffer")));
      assertThat(ci.getName(), not(containsString("com.google.j2objc.annotations")));
    }
  }

}
