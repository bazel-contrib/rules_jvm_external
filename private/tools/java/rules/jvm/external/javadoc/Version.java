package rules.jvm.external.javadoc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

/**
 * Compares two java version numbers by splitting on periods and comparing
 * numerical values. Only designed for handling java version numbers, and
 * so isn't generalised or particularly robust.
 * <p>
 * Replaced by {@code Runtime.Version} in Java 9, but we need to support
 * java 8 too.
 */
public class Version implements Comparable<Version> {

  private int[] segments;

  private Version(int[] segments) {
    this.segments = segments;
  }

  public static Version parse(String versionString) {
    String[] parts = versionString.split("\\.", 4);

    int[] segments = new int[4];
    Arrays.fill(segments, 0);

    for (int i = 0; i < parts.length; i++) {
      try {
        segments[i] = Integer.parseInt(parts[i]);
      } catch (NumberFormatException e) {
        // Do nothing. We already have a `0` value filled
      }
    }

    return new Version(segments);
  }

  @Override
  public int compareTo(Version o) {
    // Segments is the same length in all versions
    for (int i = 0; i < segments.length; i++) {
      if (segments[i] != o.segments[i]) {
        return segments[i] > o.segments[i] ? 1 : -1;
      }
    }
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Version)) {
      return false;
    }

    Version that = (Version) o;
    return Arrays.equals(this.segments, that.segments);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(segments);
  }
}
