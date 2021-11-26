package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.reporting.legacy.LegacyReportingUtils;

import javax.xml.stream.XMLStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;

class TestResult extends BaseResult {
  private final TestPlan testPlan;

  public TestResult(TestPlan testPlan, TestIdentifier id) {
    super(id);
    this.testPlan = testPlan;
  }

  public boolean isError() {
    TestExecutionResult result = getResult();
    if (result == null || result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
      return false;
    }

    return result.getThrowable().map(thr -> thr instanceof AssertionError).orElse(false);
  }

  public boolean isFailure() {
    TestExecutionResult result = getResult();
    if (result == null || result.getStatus() == TestExecutionResult.Status.SUCCESSFUL) {
      return false;
    }

    return result.getThrowable().map(thr -> (!(thr instanceof AssertionError))).orElse(false);
  }

  @Override
  public void toXml(XMLStreamWriter xml) {
    DecimalFormat decimalFormat = new DecimalFormat("#.##");
    decimalFormat.setRoundingMode(RoundingMode.HALF_UP);

    write(() -> {
      // Massage the name
      String name = getTestId().getLegacyReportingName();
      int index = name.indexOf('(');
      if (index != -1) {
        name = name.substring(0, index);
      }

      xml.writeStartElement("testcase");
      xml.writeAttribute("name", name);
      xml.writeAttribute("classname", LegacyReportingUtils.getClassName(testPlan, getTestId()));
      xml.writeAttribute("time", decimalFormat.format(getDuration().toMillis() / 1000f));

      if (isFailure() || isError()) {
        Throwable throwable = getResult().getThrowable().orElse(null);

        xml.writeStartElement(isFailure() ? "failure" : "error");
        if (throwable == null) {
          // Stub out the values
          xml.writeAttribute("message", "unknown cause");
          xml.writeAttribute("type", RuntimeException.class.getName());
          xml.writeEndElement();
          return;
        }

        xml.writeAttribute("message", throwable.getMessage());
        xml.writeAttribute("type", throwable.getClass().getName());

        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));

        xml.writeCData(stringWriter.toString());

        xml.writeEndElement();
      }

      String stdout = getStdOut();
      if (stdout != null) {
        xml.writeStartElement("system-out");
        xml.writeCData(stdout);
        xml.writeEndElement();
      }

      String stderr = getStdErr();
      if (stderr != null) {
        xml.writeStartElement("system-err");
        xml.writeCData(stderr);
        xml.writeEndElement();
      }

      xml.writeEndElement();
    });
  }

  @Override
  protected Optional<BaseResult> get(TestIdentifier id) {
    return getTestId().equals(id) ? Optional.of(this) : Optional.empty();
  }
}
