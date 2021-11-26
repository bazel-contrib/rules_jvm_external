package com.github.bazelbuild.rules_jvm_external.junit5;

/**
 * Test bootstrapper. This class only depends on the JRE (java 11+) and will
 * ensure that the required dependencies for a junit5 test are on the
 * classpath before creating the actual runner. In this way we can offer a
 * useful error message to people, which is always nice, right?
 * <p>
 * Most of the configuration information can be found on this page in the
 * <a href="https://docs.bazel.build/versions/master/test-encyclopedia.html">
 * Test Encyclopedia</a>.
 */
public class JUnit5Runner {

  private static final String JUNIT5_RUNNER_CLASS = "com.github.bazelbuild.rules_jvm_external.junit5.ActualRunner";

  public static void main(String[] args) {
    var testSuite = System.getProperty("bazel.test_suite");

    if (testSuite == null || testSuite.isBlank()) {
      System.err.println("No test suite specified");
      System.exit(2); // Same error code as Bazel's own test runner
    }

    detectJUnit5Classes();

    try {
      var constructor = Class.forName(JUNIT5_RUNNER_CLASS)
        .asSubclass(RunsTest.class)
        .getConstructor();
      var runsTest = constructor.newInstance();
      if (!runsTest.run(testSuite)) {
        System.exit(2);
      }
    } catch (ReflectiveOperationException e) {
      e.printStackTrace(System.err);
      System.err.println("Unable to create delegate test runner");
      System.exit(2);
    }

    // Exit manually. If we don't do this then tests which hold resources
    // such as Threads may prevent us from exiting properly.
    System.exit(0);
  }

  private static void detectJUnit5Classes() {
    checkClass(
      "org.junit.jupiter.engine.JupiterTestEngine",
      "org.junit.jupiter:junit-jupiter-engine");
    checkClass(
      "org.junit.platform.commons.JUnitException",
      "org.junit.platform:junit-platform-commons");
    checkClass(
      "org.junit.platform.engine.ExecutionRequest",
      "org.junit.platform:junit-platform-engine");
    checkClass(
      "org.junit.platform.launcher.TestPlan",
      "org.junit.platform:junit-platform-launcher");
    checkClass(
      "org.junit.platform.reporting.legacy.LegacyReportingUtils",
      "org.junit.platform:junit-platform-reporting");
  }

  private static void checkClass(String className, String containedInDependency) {
    try {
      Class.forName(className);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(
        String.format("JUnit 5 test runner is missing a dependency on `artifact(\"%s\")`%n", containedInDependency));
    }
  }

}
