package com.github.bazelbuild.rules_jvm_external.junit5;

@FunctionalInterface
public interface RunsTest {
  boolean run(String testClassName);
}
