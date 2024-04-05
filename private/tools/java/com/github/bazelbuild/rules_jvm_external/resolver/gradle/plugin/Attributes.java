package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import static org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE;

import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;

public class Attributes {

  private Attributes() {
    // Utility class
  }

  public static boolean isPlatform(AttributeContainer attributes) {
    Category category = attributes.getAttribute(CATEGORY_ATTRIBUTE);
    if (category == null) {
      return false;
    }

    return Category.REGULAR_PLATFORM.equals(category.getName())
        || Category.ENFORCED_PLATFORM.equals(category.getName());
  }
}
