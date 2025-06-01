package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/** Utility class to handle POM files */
public class PomUtil {
  public static String extractPackagingFromPom(File pomFile) {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(false);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(pomFile);
      NodeList packagingNodes = doc.getElementsByTagName("packaging");
      if (packagingNodes.getLength() > 0) {
        return packagingNodes.item(0).getTextContent().trim();
      }
    } catch (Exception e) {
      // we can gracefully fail here
    }
    return "jar"; // default if absent
  }
}
