package com.github.bazelbuild.rules_jvm_external.resolver.events;

public class LogEvent implements Event {

  private final String source;
  private final String message;
  private final String detail;

  public LogEvent(String source, String message, String detail) {
    this.source = source;
    this.message = message;
    this.detail = detail;
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder(source).append(": ").append(message);
    if (detail != null && System.getenv("RJE_VERBOSE") != null) {
      str.append("\n").append(detail);
    }
    return str.toString();
  }
}
