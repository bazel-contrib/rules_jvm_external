package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherConstants;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.nio.file.StandardOpenOption.DELETE_ON_CLOSE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.platform.launcher.EngineFilter.includeEngines;

public class ActualRunner implements RunsTest {

  @Override
  public boolean run(String testClassName) {
    var out = System.getenv("XML_OUTPUT_FILE");
    Path xmlOut;
    try {
      xmlOut = out != null ? Paths.get(out) : Files.createTempFile("test", ".xml");
      Files.createDirectories(xmlOut.getParent());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    try (var bazelJunitXml = new BazelJUnitOutputListener(xmlOut)) {
      var summary = new CommandLineSummary();

      LauncherConfig config = LauncherConfig.builder()
        .addTestExecutionListeners(bazelJunitXml, summary)
        .build();

      var classSelector = DiscoverySelectors.selectClass(testClassName);

      var request = LauncherDiscoveryRequestBuilder.request()
        .selectors(List.of(classSelector))
        .filters(includeEngines(
          "junit-jupiter",
          "junit-vintage"))
        .configurationParameter(LauncherConstants.CAPTURE_STDERR_PROPERTY_NAME, "true")
        .configurationParameter(LauncherConstants.CAPTURE_STDOUT_PROPERTY_NAME, "true");

      String filter = System.getenv("TESTBRIDGE_TEST_ONLY");
      request.filters(new PatternFilter(filter));

      File exitFile = getExitFile();
      var originalSecurityManager = System.getSecurityManager();
      TestRunningSecurityManager testSecurityManager = new TestRunningSecurityManager(originalSecurityManager);
      try {
        System.setSecurityManager(testSecurityManager);
        var launcher = LauncherFactory.create(config);
        launcher.execute(request.build());
     } finally {
        testSecurityManager.allowRemoval();
        System.setSecurityManager(originalSecurityManager);
      }
      deleteExitFile(exitFile);

      try (PrintWriter writer = new PrintWriter(System.out)) {
        summary.writeTo(writer);
      }

      return summary.getFailureCount() == 0;
    }
  }

  private File getExitFile() {
    String exitFileName = System.getenv("TEST_PREMATURE_EXIT_FILE");
    if (exitFileName == null) {
      return null;
    }

    File exitFile = new File(exitFileName);
    try {
      Files.write(exitFile.toPath(), "".getBytes(), WRITE, DELETE_ON_CLOSE, TRUNCATE_EXISTING);
    } catch (IOException e) {
      return null;
    }

    return exitFile;
  }

  private void deleteExitFile(File exitFile) {
    if (exitFile != null) {
      try {
        exitFile.delete();
      } catch (Throwable t) {
        // Ignore.
      }
      }
  }
}
