package com.jvm.external.jvm_export;

/**
 * A self-contained library with no external dependencies, published as a non-timestamped SNAPSHOT
 * to the local Maven repository to exercise snapshot pinning in the gradle resolver.
 */
public class Snapshot {

  public String greeting() {
    return "Hello from a non-timestamped snapshot!";
  }
}
