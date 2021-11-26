package com.github.bazelbuild.rules_jvm_external.junit5;

import org.junit.platform.engine.reporting.ReportEntry;

import java.util.Map;

import static org.junit.platform.launcher.LauncherConstants.STDERR_REPORT_ENTRY_KEY;
import static org.junit.platform.launcher.LauncherConstants.STDOUT_REPORT_ENTRY_KEY;

class EntryDetails {

  private EntryDetails() {
    // Utility class
  }

  public static String getStdOut(ReportEntry entry) {
    return getReportEntryValue(entry, STDOUT_REPORT_ENTRY_KEY);
  }

  public static String getStdErr(ReportEntry entry) {
    return getReportEntryValue(entry, STDERR_REPORT_ENTRY_KEY);
  }

  private static String getReportEntryValue(ReportEntry entry, String key) {
    return entry.getKeyValuePairs().entrySet().stream()
      .filter(e -> key.equals(e.getKey()))
      .map(Map.Entry::getValue)
      .findFirst()
      .orElse(null);
  }

}
