package com.github.bazelbuild.rules_jvm_external;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CoordinatesTest {

  @Test
  public void coordinatesAreConsistentWhenDefaultValuesAreUsed() {
    Coordinates plain = new Coordinates("com.example:foo:1.2.3");
    Coordinates fancy = new Coordinates("com.example:foo:jar:jar:1.2.3");

    assertEquals(plain, fancy);
    assertEquals(plain.hashCode(), fancy.hashCode());
  }

  @Test
  public void coordinatesAreConsistentWhenEllidedValuesAreUsed() {
    Coordinates plain = new Coordinates("com.example:foo:1.2.3");
    Coordinates fancy = new Coordinates("com.example:foo:::1.2.3");

    assertEquals(plain, fancy);
    assertEquals(plain.hashCode(), fancy.hashCode());
  }
}
