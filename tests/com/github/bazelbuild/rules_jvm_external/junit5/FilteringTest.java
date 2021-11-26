package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.config.DefaultJupiterConfiguration;
import org.junit.jupiter.engine.config.JupiterConfiguration;
import org.junit.jupiter.engine.descriptor.ClassTestDescriptor;
import org.junit.jupiter.engine.descriptor.JupiterEngineDescriptor;
import org.junit.jupiter.engine.descriptor.TestMethodTestDescriptor;
import org.junit.platform.engine.ConfigurationParameters;
import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.UniqueId;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class FilteringTest {

  private JupiterEngineDescriptor engineDescriptor;
  private ClassTestDescriptor classTestDescriptor;
  private TestMethodTestDescriptor testMethodTestDescriptor;

  @BeforeEach
  public void setup() throws NoSuchMethodException {
    UniqueId engine = UniqueId.forEngine("engine");
    JupiterConfiguration config = new DefaultJupiterConfiguration(new EmptyConfigParameters());

    engineDescriptor = new JupiterEngineDescriptor(engine, config);
    UniqueId classId = engine.append("class", "foo");
    classTestDescriptor = new ClassTestDescriptor(classId, JUnit5StyleTest.class, config);
    Method method = JUnit5StyleTest.class.getMethod("alwaysPasses");
    testMethodTestDescriptor = new TestMethodTestDescriptor(classId.append("method", "bar"), JUnit5StyleTest.class, method, config);
  }

  @Test
  public void ifFilterIsNotSetAllTestsShouldBeAccepted() {
    PatternFilter filter = new PatternFilter(null);

    FilterResult engineResult = filter.apply(engineDescriptor);
    assertTrue(engineResult.included());

    FilterResult classResult = filter.apply(classTestDescriptor);
    assertTrue(classResult.included());

    FilterResult testResult = filter.apply(testMethodTestDescriptor);
    assertTrue(testResult.included());
  }

  @Test
  public void ifFilterIsSetButEmptyAllTestsShouldBeAccepted() {
    PatternFilter filter = new PatternFilter("");

    FilterResult engineResult = filter.apply(engineDescriptor);
    assertTrue(engineResult.included());

    FilterResult classResult = filter.apply(classTestDescriptor);
    assertTrue(classResult.included());

    FilterResult testResult = filter.apply(testMethodTestDescriptor);
    assertTrue(testResult.included());
  }

  @Test
  public void ifFilterIsSetButNoTestsMatchTheContainersAreIncluded() {
    PatternFilter filter = new PatternFilter("com.example.will.never.Match#");

    FilterResult engineResult = filter.apply(engineDescriptor);
    assertTrue(engineResult.included());

    FilterResult classResult = filter.apply(classTestDescriptor);
    assertTrue(classResult.included());

    FilterResult testResult = filter.apply(testMethodTestDescriptor);
    assertFalse(testResult.included());
  }

  @Test
  public void shouldIncludeATestMethodIfTheFilterIsJustTheClassName() {
    PatternFilter filter = new PatternFilter(JUnit5StyleTest.class.getName().replace("$", "\\$") + "#");

    FilterResult testResult = filter.apply(testMethodTestDescriptor);
    assertTrue(testResult.included());
  }

  @Test
  public void shouldNotIncludeATestMethodIfTheFilterDoesNotMatchTheMethodName() {
    PatternFilter filter = new PatternFilter("#foo");

    FilterResult testResult = filter.apply(testMethodTestDescriptor);
    assertFalse(testResult.included());
  }

  @Test
  public void shouldIncludeATestMethodIfTheFilterMatchesTheMethodName() {
    PatternFilter filter = new PatternFilter("#alwaysPasses");

    FilterResult testResult = filter.apply(testMethodTestDescriptor);
    assertTrue(testResult.included());
  }

  private static class EmptyConfigParameters implements ConfigurationParameters {
    @Override
    public Optional<String> get(String key) {
      return Optional.empty();
    }

    @Override
    public Optional<Boolean> getBoolean(String key) {
      return Optional.empty();
    }

    @Override
    public int size() {
      return 0;
    }
  }

  private static class JUnit5StyleTest {
    @Test
    public void alwaysPasses() {
    }
  }
}
