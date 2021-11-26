package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BazelJUnitOutputListener implements TestExecutionListener, Closeable {

  private static final Logger LOG = Logger.getLogger(BazelJUnitOutputListener.class.getName());
  private final XMLStreamWriter xml;
  private Set<RootContainer> roots;

  public BazelJUnitOutputListener(Path xmlOut) {
    // Outputs from tests can be pretty large, so rather than hold them
    // in memory, write to the output xml asap.

    try {
      Files.createDirectories(xmlOut.getParent());
      BufferedWriter writer = Files.newBufferedWriter(xmlOut, UTF_8);
      xml = XMLOutputFactory.newDefaultFactory().createXMLStreamWriter(writer);
      xml.writeStartDocument("UTF-8", "1.0");
    } catch (IOException | XMLStreamException e) {
      throw new IllegalStateException("Unable to create output file", e);
    }
  }

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    if (roots != null) {
      throw new IllegalStateException("Test plan is currently executing");
    }

    roots = testPlan.getRoots().stream()
      .map(root -> new RootContainer(root, testPlan))
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    if (roots == null) {
      throw new IllegalStateException("Test plan is not currently executing");
    }

    try {
      xml.writeStartElement("testsuites");
      roots.forEach(root -> root.toXml(xml));
      xml.writeEndElement();
    } catch (XMLStreamException e) {
      throw new RuntimeException(e);
    } finally {
      roots = null;
    }
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    roots.forEach(root -> root.markStarted(testIdentifier));
  }

  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    roots.forEach(root -> root.markSkipped(testIdentifier, reason));
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    roots.forEach(root -> root.markFinished(testIdentifier, testExecutionResult));
  }

  @Override
  public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
    roots.forEach(root -> root.addReportingEntry(testIdentifier, entry));
  }

  @Override
  public void close() {
    try {
      xml.writeEndDocument();
      xml.close();
    } catch (XMLStreamException e) {
      LOG.log(Level.SEVERE, "Unable to close xml output", e);
    }
  }
}
