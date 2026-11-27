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

package com.github.bazelbuild.rules_jvm_external.resolver.lockfile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** The canonical JSON representation shared by v4 hashes and metadata ordering. */
final class V4Json {
  private static final Gson COMPACT =
      new GsonBuilder().serializeNulls().disableHtmlEscaping().create();
  private static final Gson PRETTY =
      new GsonBuilder().serializeNulls().disableHtmlEscaping().setPrettyPrinting().create();

  private V4Json() {}

  static String compact(Object value) {
    return COMPACT.toJson(sorted(value));
  }

  static String pretty(Object value) {
    return PRETTY.toJson(sorted(value)).replace("\n", "\n  ");
  }

  static String prettyInOrder(Object value) {
    return PRETTY.toJson(value).replace("\n", "\n  ");
  }

  static Object sorted(Object value) {
    if (value instanceof Map) {
      Map<String, Object> result = new TreeMap<>();
      ((Map<?, ?>) value).forEach((key, nested) -> result.put((String) key, sorted(nested)));
      return result;
    }
    if (value instanceof Collection) {
      List<Object> result = new ArrayList<>();
      ((Collection<?>) value).forEach(item -> result.add(sorted(item)));
      return result;
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  static <T> Map<String, T> map(Object value) {
    return value == null ? Map.of() : (Map<String, T>) value;
  }

  @SuppressWarnings("unchecked")
  static <T> Collection<T> items(Object value) {
    return value == null ? List.of() : (Collection<T>) value;
  }
}
