package com.github.bazelbuild.rules_jvm_external.junit5;

import java.security.Permission;

class TestRunningSecurityManager extends SecurityManager {
  private static final RuntimePermission SET_SECURITY_MANAGER_PERMISSION =
      new RuntimePermission("setSecurityManager");
  private final SecurityManager existing;
  private boolean allowRemoval = false;

  TestRunningSecurityManager(SecurityManager existing) {
    this.existing = existing;
  }

  void allowRemoval() {
    allowRemoval = true;
  }

  @Override
  public void checkExit(int status) {
    throw new SecurityException("Attempt to call System.exit");
  }

  @Override
  public void checkPermission(Permission perm) {
    if (SET_SECURITY_MANAGER_PERMISSION.equals(perm)) {
      if (allowRemoval) {
        return;
      }
      throw new SecurityException("Replacing the security manager is not allowed");
    }

    if (existing != null) {
      existing.checkPermission(perm);
    }
  }

  @Override
  public void checkPermission(Permission perm, Object context) {
    // The default implementation of the SecurityManager checks to see
    // if the `context` is an `AccessControlContext`, and if it is calls
    // `checkPermission` on that. However, when there's no security
    // manager installed, there's never a problem. We're going to pretend
    // that we are "no security manager" installed and just allow things
    // to happen because that's how most people are running their tests.

    if (existing != null) {
      existing.checkPermission(perm, context);
    }
  }
}
