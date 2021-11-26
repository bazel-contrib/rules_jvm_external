package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.jupiter.api.Test;

import java.security.Permission;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class TestRunningSecurityManagerTest {

  @Test
  void shouldStifleSystemExitCalls() {
    SecurityManager sm = new TestRunningSecurityManager(null);
    assertThrows(SecurityException.class, () -> sm.checkExit(2));
  }

  @Test
  void shouldDelegateToExistingSecurityManagerIfPresent() {
    SecurityManager permissive = new TestRunningSecurityManager(null);
    Permission permission = new RuntimePermission("example.permission");
    SecurityManager restrictive = new TestRunningSecurityManager(
      new SecurityManager() {
        @Override
        public void checkPermission(Permission perm) {
          if (permission == perm) {
              throw new SecurityException("Oh noes!");
          }
        }
      });

    // This should do nothing, but if an exception is thrown, our test fails.
    permissive.checkPermission(permission);

    // Whereas this delegates down to the custom security manager
    assertThrows(SecurityException.class, () -> restrictive.checkPermission(permission));
  }
}
