// Copyright 2024 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver.gradle;

import java.net.MalformedURLException;
import java.net.URI;

/** Models a maven repository that is added to a gradle build script */
public class Repository {
  public final URI uri;
  public final boolean requiresAuth;
  public final String usernameProperty;
  public final String passwordProperty;
  private final String password;
  private final String username;

  public Repository(URI uri) {
    this(uri, false, null, null);
  }

  public Repository(URI uri, boolean requiresAuth, String username, String password) {
    this.uri = uri;
    this.requiresAuth = requiresAuth;
    String host = URI.create(getUrl()).getHost();
    this.username = username;
    this.password = password;
    this.usernameProperty = host + "UserName";
    this.passwordProperty = host + "Password";
  }

  public String getUsernameProperty() {
    return this.usernameProperty;
  }

  public String getPasswordProperty() {
    return this.passwordProperty;
  }

  public String getPassword() {
    return password;
  }

  public String getUsername() {
    return username;
  }

  public String getUrl() {
    try {
      return uri.toURL().toString();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
}
