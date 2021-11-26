package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestIdentifier;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

abstract class BaseResult {
  private final TestIdentifier testId;
  private final List<ReportEntry> entries = new LinkedList<>();
  private Instant start = Instant.now();
  private Instant stop = Instant.now();
  private TestExecutionResult result;
  private String skipReason;

  protected BaseResult(TestIdentifier testId) {
    this.testId = Objects.requireNonNull(testId);
  }

  public void markStarted() {
    start = Instant.now();
  }

  public void markSkipped(String reason) {
    stop = Instant.now();
    this.skipReason = reason;
  }

  public void markFinished(TestExecutionResult testExecutionResult) {
    stop = Instant.now();
    this.result = testExecutionResult;
  }

  protected void addReportEntry(ReportEntry entry) {
    entries.add(entry);
  }

  protected String getStdOut() {
    return getReportEntryValue(EntryDetails::getStdOut);
  }

  protected String getStdErr() {
    return getReportEntryValue(EntryDetails::getStdErr);
  }

  private String getReportEntryValue(Function<ReportEntry, String> mapper) {
    return entries.stream().map(mapper).filter(Objects::nonNull).findFirst().orElse(null);
  }

  protected Duration getDuration() {
    return Duration.between(start, stop);
  }

  protected String getSkipReason() {
    return skipReason;
  }

  protected TestExecutionResult getResult() {
    return result;
  }

  protected TestIdentifier getTestId() {
    return testId;
  }

  public abstract void toXml(XMLStreamWriter xml);

  protected void write(WriteXml toWrite) {
    try {
      toWrite.write();
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract Optional<BaseResult> get(TestIdentifier id);

  @FunctionalInterface
  protected interface WriteXml {
    void write() throws XMLStreamException;
  }
}
