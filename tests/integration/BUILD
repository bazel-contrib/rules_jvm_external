load("@gmaven_rules//:defs.bzl", "artifact")

java_binary(
    name = "java_binary_test",
    main_class = "not.used",
    runtime_deps = [
        artifact("com.google.code.gson:gson"),
        artifact("com.google.guava:guava", repository_name = "other_maven"),
        artifact("com.google.inject:guice"),
        artifact("commons-io:commons-io"),
        artifact("io.reactivex.rxjava2:rxjava"),
        artifact("junit:junit"),
        artifact("org.hamcrest:java-hamcrest"),
        artifact("org.mockito:mockito-core"),
        artifact("org.springframework:spring-core"),
    ],
)

android_binary(
    name = "android_binary_test",
    custom_package = "not.used",
    manifest = "AndroidManifest.xml",
    multidex = "native",
    deps = [
        artifact("androidx.test.espresso:espresso-core"),
        artifact("androidx.test.ext:junit"),
        artifact("androidx.test:runner"),
        artifact("com.google.android.gms:play-services-maps"),
        artifact("com.google.code.gson:gson"),
        artifact("com.google.guava:guava"),
        artifact("io.reactivex.rxjava2:rxjava"),
        artifact("org.mockito:mockito-core"),
        # The Android design AAR is extremely large, and depends on transitive
        # resources from its AAR deps. If the internal dependency graph isn't
        # wired up properly, building this will fail spectacularly. This makes
        # for a good load testing target.
        artifact("com.android.support:design"),
    ],
)
