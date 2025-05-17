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
        this.listener = Objects.requireNonNull(listener);
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