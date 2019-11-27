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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** A tool to compute the sha256 hash of a file. */
public class Hasher {

  public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
    // Since this tool is for private usage, just do a simple assertion for the filename argument.
    if (args.length != 1) {
      throw new IllegalArgumentException("Please specify the path of the file to be hashed.");
    }

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
    byte[] hashDigest = digest.digest();
    StringBuilder hexString = new StringBuilder();
    for (int i = 0; i < hashDigest.length; i++) {
      String hex = Integer.toHexString(0xff & hashDigest[i]);
      if (hex.length() == 1) hexString.append('0');
      hexString.append(hex);
    }
    // Print without a newline so consumers don't have to trim the string.
    System.out.print(hexString);
  }
}
