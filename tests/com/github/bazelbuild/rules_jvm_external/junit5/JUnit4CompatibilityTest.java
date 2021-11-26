package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

public class JUnit4CompatibilityTest {

  @Test
  public void shouldDetectJUnit4Tests() {
    CommandLineSummary summary = new CommandLineSummary();

    LauncherConfig config = LauncherConfig.builder()
      .addTestExecutionListeners(summary)
      .build();

    ClassSelector classSelector = DiscoverySelectors.selectClass(Junit4StyleTest.class);

    LauncherDiscoveryRequestBuilder request = LauncherDiscoveryRequestBuilder.request()
      .selectors(classSelector)
      .filters(includeEngines(
        "junit-jupiter",
        "junit-vintage"))
      .configurationParameter(LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME, "true")
      .configurationParameter(LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME, "true");

    Launcher launcher = LauncherFactory.create(config);
    launcher.execute(request.build());

    assertEquals(1, summary.getFailureCount());
  }

  public static class Junit4StyleTest {

    @org.junit.Test
    public void shouldPass() {
    }

    @org.junit.Test
    public void shouldFail() {
      org.junit.Assert.fail("Oh noes too!");
    }
  }
}
