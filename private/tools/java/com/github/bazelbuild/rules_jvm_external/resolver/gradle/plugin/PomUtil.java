// Copyright 2025 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.bazelbuild.rules_jvm_external.resolver.gradle.plugin;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/** Utility class to handle POM files ***/
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
