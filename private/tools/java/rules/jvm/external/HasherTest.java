// Copyright 2019 The Bazel Authors. All rights reserved.
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
package rules.jvm.external;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class HasherTest {

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Test
  public void sha256_emptyFile() throws IOException, NoSuchAlgorithmException {
    File file = tmpDir.newFile("test.file");

    // "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855" is sha of null content.
    assertThat(Hasher.sha256(file), equalTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
  }

  @Test
  public void sha256_helloWorldFile() throws IOException, NoSuchAlgorithmException {
    File file = tmpDir.newFile("test.file");

    try (PrintStream out = new PrintStream(new FileOutputStream(file))) {
      out.print("Hello World!");
    }

    assertThat(Hasher.sha256(file), equalTo("7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069"));
  }
}
