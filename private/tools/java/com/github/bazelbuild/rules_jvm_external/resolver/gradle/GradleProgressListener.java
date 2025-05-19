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

import com.github.bazelbuild.rules_jvm_external.resolver.events.DownloadEvent;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.download.FileDownloadFinishEvent;
import org.gradle.tooling.events.download.FileDownloadStartEvent;

import java.util.Objects;

/**
 * Listens to download start/ending events for artifacts from the Gradle daemon
 * while resolving dependencies.
 */
public class GradleProgressListener implements ProgressListener {
    private static final String DOWNLOAD = "Download ";
    private final EventListener listener;

    public GradleProgressListener(EventListener listener) {
        this.listener = listener;
    }

    @Override
    public void statusChanged(ProgressEvent progressEvent) {
        if (progressEvent instanceof FileDownloadStartEvent) {
            String name = progressEvent.getDescriptor().getName();
            if (name == null) {
                return;
            }
            if (name.startsWith(DOWNLOAD)) {
                name = name.substring(DOWNLOAD.length());
            }

            listener.onEvent(new DownloadEvent(DownloadEvent.Stage.STARTING, name));
        } else if (progressEvent instanceof FileDownloadFinishEvent) {
            String name = progressEvent.getDescriptor().getName();
            if (name == null) {
                return;
            }
            if (name.startsWith(DOWNLOAD)) {
                name = name.substring(DOWNLOAD.length());
            }
            listener.onEvent(new DownloadEvent(DownloadEvent.Stage.COMPLETE, name));
        }
    }
}