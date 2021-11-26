package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import javax.xml.stream.XMLStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public class RootContainer extends BaseResult {

  // Insertion order matters when we come to output the results
  private final List<TestSuiteResult> suites = new LinkedList<>();

  public RootContainer(TestIdentifier rootId, TestPlan testPlan) {
    super(rootId);

    testPlan.getChildren(rootId).forEach(child -> suites.add(createSuite(child, testPlan)));
  }

  public void markStarted(TestIdentifier testIdentifier) {
    get(testIdentifier).ifPresent(BaseResult::markStarted);
  }

  public void markSkipped(TestIdentifier testIdentifier, String reason) {
    get(testIdentifier).ifPresent(result -> result.markSkipped(reason));
  }

  public void markFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    get(testIdentifier).ifPresent(result -> result.markFinished(testExecutionResult));
  }

  public void addReportingEntry(TestIdentifier testIdentifier, ReportEntry entry) {
    get(testIdentifier).ifPresent(result -> result.addReportEntry(entry));
  }

  protected Optional<BaseResult> get(TestIdentifier testIdentifier) {
    if (getTestId().equals(testIdentifier)) {
      return Optional.of(this);
    }

    return suites.stream()
      .map(suite -> suite.get(testIdentifier))
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst();
  }

  public void toXml(XMLStreamWriter xml) {
    write(() -> suites.forEach(suite -> suite.toXml(xml)));
  }

  private TestSuiteResult createSuite(TestIdentifier suiteId, TestPlan plan) {
    TestSuiteResult suite = new TestSuiteResult(suiteId);
    for (TestIdentifier child : plan.getChildren(suiteId)) {
      if (child.isContainer()) {
        suite.add(createSuite(child, plan));
      } else {
        suite.add(new TestResult(plan, child));
      }
    }
    return suite;
  }
}
