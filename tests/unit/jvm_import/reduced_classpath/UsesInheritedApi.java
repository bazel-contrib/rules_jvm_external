package com.github.bazelbuild.rules_jvm_external.jvm_import;

import com.google.auth.oauth2.GoogleCredentials;

public class UsesInheritedApi {

  // `hasRequestMetadata` is declared by `com.google.auth.Credentials`, which lives in
  // `google-auth-library-credentials`, a dependency of `google-auth-library-oauth2-http`.
  public static boolean hasRequestMetadata(GoogleCredentials credentials) {
    return credentials.hasRequestMetadata();
  }
}
