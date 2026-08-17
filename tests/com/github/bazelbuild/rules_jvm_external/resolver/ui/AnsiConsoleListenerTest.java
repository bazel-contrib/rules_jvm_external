// Copyright 2026 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AnsiConsoleListenerTest {

  @Test
  public void leavesLineShorterThanWidthUnchanged() {
    assertEquals("short", AnsiConsoleListener.elideLine(6, "short"));
  }

  @Test
  public void leavesLineMatchingWidthUnchanged() {
    assertEquals("exact", AnsiConsoleListener.elideLine(5, "exact"));
  }

  @Test
  public void elidesLinesJustOverWidthWithoutExceedingWidth() {
    String line = "abcdefghijklmnopqrstuvwxyz";

    for (int extra : new int[] {1, 2, 3, 6}) {
      int width = line.length() - extra;
      String elided = AnsiConsoleListener.elideLine(width, line);
      assertEquals(width, elided.length());
      assertEquals("..." + line.substring(line.length() - width + 3), elided);
    }
  }

  @Test
  public void keepsTailWhenWidthCannotFitEllipsis() {
    assertEquals("", AnsiConsoleListener.elideLine(-1, "abcdef"));
    assertEquals("", AnsiConsoleListener.elideLine(0, "abcdef"));
    assertEquals("f", AnsiConsoleListener.elideLine(1, "abcdef"));
    assertEquals("ef", AnsiConsoleListener.elideLine(2, "abcdef"));
    assertEquals("def", AnsiConsoleListener.elideLine(3, "abcdef"));
  }

  @Test
  public void accountsForDownloadIndentWithinTerminalWidth() {
    int terminalWidth = 20;
    String line =
        "    "
            + AnsiConsoleListener.elideLine(
                terminalWidth - 4, "https://example.com/very/long/file.jar");

    assertEquals(terminalWidth, line.length());
  }
}
