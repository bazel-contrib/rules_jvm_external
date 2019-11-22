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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A tool to compute the sha256 hash of a file.
 */
public class Hasher {

  public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
    // Since this tool is for private usage, just do a simple assertion for the filename argument.
    assert (args.length == 1) : "Please specify the path of the file to hash.";

    String filename = args[0];
    byte[] buffer = new byte[8192];
    int count;
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    try (BufferedInputStream bufferedInputStream =
        new BufferedInputStream(new FileInputStream(filename))) {
      while ((count = bufferedInputStream.read(buffer)) > 0) {
        digest.update(buffer, 0, count);
      }
    }
    // Convert digest byte array to a hex string.
    StringBuilder hash = new StringBuilder(new BigInteger(1, digest.digest()).toString(16));
    // Left-pad with zeros until the string is 32 characters long.
    while (hash.length() < 33) {
      hash.insert(0, '0');
    }
    System.out.print(hash); // Print without a newline so consumers don't have to trim the string.
  }
}
