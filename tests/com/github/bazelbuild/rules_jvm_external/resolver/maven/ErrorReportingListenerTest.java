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

package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import static org.junit.Assert.assertEquals;

import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositoryEvent;
import org.junit.Test;

public class ErrorReportingListenerTest {

  @Test
  public void getExceptionsShouldBeThreadSafe() throws Exception {
    ErrorReportingListener listener = new ErrorReportingListener();
    int threadCount = 8;
    int eventsPerThread = 500;
    Thread[] threads = new Thread[threadCount];

    for (int t = 0; t < threadCount; t++) {
      threads[t] = new Thread(() -> {
        for (int i = 0; i < eventsPerThread; i++) {
          RepositoryEvent.Builder builder = new RepositoryEvent.Builder(
              new DefaultRepositorySystemSession(),
              RepositoryEvent.EventType.ARTIFACT_DESCRIPTOR_MISSING);
          builder.setException(new RuntimeException("err"));
          listener.artifactDescriptorMissing(builder.build());
        }
      });
    }

    for (Thread thread : threads) thread.start();
    for (Thread thread : threads) thread.join();

    assertEquals(threadCount * eventsPerThread, listener.getExceptions().size());
  }
}
