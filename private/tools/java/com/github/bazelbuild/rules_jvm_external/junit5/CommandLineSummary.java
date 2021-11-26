package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.reporting.legacy.LegacyReportingUtils;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.platform.engine.TestExecutionResult.Status.SUCCESSFUL;

public class CommandLineSummary implements TestExecutionListener {

  private final Map<TestIdentifier, Failure> failures = Collections.synchronizedMap(new LinkedHashMap<>());
  private TestPlan testPlan;

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    this.testPlan = Objects.requireNonNull(testPlan);
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
    if (result.getStatus().equals(SUCCESSFUL) || result.getThrowable().isEmpty()) {
      failures.remove(testIdentifier);
      return;
    }

    failures.computeIfAbsent(testIdentifier, ignored -> new Failure()).setResult(result);
  }

  public void writeTo(PrintWriter writer) {
    writer.printf("Failures: %s%n", getFailureCount());

    int count = 1;
    for (Map.Entry<TestIdentifier, Failure> entry : failures.entrySet()) {
      Failure failure = entry.getValue();

      String className = LegacyReportingUtils.getClassName(testPlan, entry.getKey());

      writer.printf(
        "%d) %s (%s)%n",
        count,
        entry.getKey().getDisplayName(),
        className);

      Throwable cause = failure.getCause();
      StackTraceElement[] stackTrace = cause.getStackTrace();
      // Find the last line in the stacktrace that matches the classname

      int last = stackTrace.length - 1;
      for (int i = 0; i < stackTrace.length; i++) {
        if (className.equals(stackTrace[i].getClassName())) {
          last = i;
        }
      }

      writer.println(cause);
      for (int i = 0; i <= last; i++) {
        writer.println("\tat " + stackTrace[i]);
      }

      count++;
    }
  }

  public int getFailureCount() {
    return failures.size();
  }

  private static class Failure {
    private TestExecutionResult result;

    public Failure() {
    }

    public void setResult(TestExecutionResult result) {
      this.result = result;
    }

    public Throwable getCause() {
      return result.getThrowable().orElseThrow();
    }
  }
}
