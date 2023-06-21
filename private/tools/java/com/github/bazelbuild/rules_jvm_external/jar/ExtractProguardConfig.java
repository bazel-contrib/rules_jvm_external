package com.github.bazelbuild.rules_jvm_external.jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class ExtractProguardConfig {

    private List<String> jarToSpec;

    // Directories to search for proguard configurations.
    // The list is sorted from highest priorty to lowest priority
    // Only the first one is extracted.
    private List<String> proguardDirs = Arrays.asList(
            "META-INF/com.android.tools/r8",
            "META-INF/com.android.tools/proguard",
            "META-INF/proguard"
    );

    public ExtractProguardConfig(List<String> jarToSpec) {
        this.jarToSpec = jarToSpec;
    }

    private boolean maybeCopySpec(String jarPath, String directoryName, String spec) {
        try {
            JarFile jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith(directoryName)) {
                    File outputFile = new File(spec);
                    FileOutputStream outputStream = new FileOutputStream(outputFile);
                    jarFile.getInputStream(entry).transferTo(outputStream);

                    outputStream.close();
                    return true;
                }
            }
            jarFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void extractSpec(String jar, String spec) {
        boolean hadSpec = false;
        for (String dir : proguardDirs) {
            hadSpec = maybeCopySpec(jar, dir, spec);
            if (hadSpec) {
                break;
            }
        }

        if (!hadSpec) {
            try {
                File file = new File(spec);
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void run() {
        for (String js : jarToSpec) {
            String[] parts = js.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid jar_to_spec value: " + js);
            }
            extractSpec(parts[0], parts[1]);
        }
    }

    public static void main(String[] args) {
        List<String> jarToSpec = new ArrayList();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--jar_to_spec")) {
                if (i + 1 < args.length) {
                    jarToSpec.add(args[i + 1]);
                    i++; 
                }
            }
        }

        new ExtractProguardConfig(jarToSpec).run();
    }
}
