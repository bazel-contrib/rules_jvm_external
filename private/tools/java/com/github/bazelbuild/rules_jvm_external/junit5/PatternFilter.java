package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.PostDiscoveryFilter;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Attempts to mirror the logic from Bazel's own
 * com.google.testing.junit.junit4.runner.RegExTestCaseFilter, which forms
 * a string of the test class name and the method name.
 */
public class PatternFilter implements PostDiscoveryFilter {

  private final String rawPattern;
  private final Predicate<String> pattern;

  public PatternFilter(String pattern) {
    if (pattern == null || pattern.isEmpty()) {
      pattern = ".*";
    }

    this.rawPattern = pattern;
    this.pattern = Pattern.compile(pattern).asPredicate();
  }

  @Override
  public FilterResult apply(TestDescriptor object) {
    if (!object.isTest()) {
      return FilterResult.included("Including container: " + object.getDisplayName());
    }

    if (object.getSource().isEmpty()) {
      return FilterResult.excluded("Skipping a test without a source: " + object.getDisplayName());
    }

    TestSource source = object.getSource().get();
    String testName;
    if (source instanceof MethodSource) {
      MethodSource method = (MethodSource) source;
      testName = method.getClassName() + "#" + method.getMethodName();
    } else if (source instanceof ClassSource) {
      ClassSource clazz = (ClassSource) source;
      testName = clazz.getClassName() + "#";
    } else {
      testName = object.getDisplayName();
    }

    if (pattern.test(testName)) {
      return FilterResult.included("Matched " + testName + " by " + rawPattern);
    }

    return FilterResult.excluded("Did not match " + rawPattern);
  }
}
