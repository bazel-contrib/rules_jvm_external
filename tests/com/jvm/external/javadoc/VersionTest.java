package rules.jvm.external.javadoc;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VersionTest {

  @Test
  public void shouldIndicateThatTwoValuesAreEqual() {
    Version v1 = Version.parse("1.0");
    Version v2 = Version.parse("1.0");

    assertEquals(v1, v2);
    assertEquals(0, v1.compareTo(v2));
  }

  @Test
  public void versionsWithMinorPartsAreGreaterThanOnesWithout() {
    Version[] versions = {Version.parse("1.8.11"), Version.parse("1.8")};

    Arrays.sort(versions);
    assertArrayEquals(new Version[] {Version.parse("1.8"), Version.parse("1.8.11")}, versions);
  }

  @Test
  public void java8VersionStringIsLessThanJava11VersionString() {
    // From OpenJDK 11
    Version java11 = Version.parse("11.0.7");
    // From OpenJDK 8
    Version java8 = Version.parse("1.8.0_262");

    assertTrue(java11.compareTo(java8) > 0);
  }

  @Test
  public void java9IsLessThanJava13() {
    // From OpenJDK 9
    Version java9 = Version.parse("9.0.4");
    // From OpenJDK 13
    Version java13 = Version.parse("13.0.2");

    assertTrue(java9.compareTo(java13) < 0);
  }

  @Test
  public void java9IsGreaterThanJava8() {
    Version java9 = Version.parse("9.0.4");
    Version java8 = Version.parse("1.8.0_262");

    assertTrue(java9.compareTo(java8) > 0);
  }

}
