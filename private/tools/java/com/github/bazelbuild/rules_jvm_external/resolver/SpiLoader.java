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

package com.github.bazelbuild.rules_jvm_external.resolver;

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Consumer;

/**
 * Utility class to dynamically load and resolve SPI (Service Provider Interface) services.
 * Ensures that at most one custom service implementation is registered on the classpath.
 */
public class SpiLoader {
  private SpiLoader() {}

  /**
   * Loads a service implementation of the specified class.
   *
   * @param serviceClass the class of the service to load
   * @param defaultService the default service instance to return if no custom implementation is found
   * @param initializer a callback to initialize the loaded service
   * @param <T> the service type
   * @return the loaded service, or defaultService if none is registered
   * @throws IllegalStateException if multiple implementations are found
   */
  public static <T> T load(Class<T> serviceClass, T defaultService, Consumer<T> initializer) {
    ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
    Iterator<T> iterator = loader.iterator();
    if (iterator.hasNext()) {
      T service = iterator.next();
      if (iterator.hasNext()) {
        java.util.List<String> names = new java.util.ArrayList<>();
        names.add(service.getClass().getName());
        while (iterator.hasNext()) {
          names.add(iterator.next().getClass().getName());
        }
        String foundServices = com.google.common.base.Joiner.on(", ").join(names);
        throw new IllegalStateException(
            "Multiple " + serviceClass.getSimpleName() + " implementations found via SPI: [" + foundServices + "]");
      }
      initializer.accept(service);
      return service;
    }
    return defaultService;
  }
}
