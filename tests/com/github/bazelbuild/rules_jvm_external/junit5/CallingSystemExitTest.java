package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CallingSystemExitTest {

  @Test
  public void shouldBeAbleToCallSystemExitInATest() {
    assertThrows(SecurityException.class, () -> System.exit(2));
  }
}
