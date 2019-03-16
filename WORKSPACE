workspace(name = "rules_jvm_external")

android_sdk_repository(name = "androidsdk")

local_repository(
    name = "rules_jvm_external",
    path = ".",
)

load("@rules_jvm_external//:gmaven.bzl", "gmaven_rules")

gmaven_rules()

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Begin Stardoc dependencies

load("@bazel_tools//tools/build_defs/repo:git.bzl", "git_repository")

git_repository(
    name = "io_bazel_skydoc",
    remote = "https://github.com/bazelbuild/skydoc.git",
    commit = "f57c9880b9033241ad341ffc17a88d804cdfde3a",
)

load("@io_bazel_skydoc//:setup.bzl", "skydoc_repositories")
skydoc_repositories()

load("@io_bazel_rules_sass//:package.bzl", "rules_sass_dependencies")
rules_sass_dependencies()

load("@build_bazel_rules_nodejs//:defs.bzl", "node_repositories")
node_repositories()

load("@io_bazel_rules_sass//:defs.bzl", "sass_repositories")
sass_repositories()

# End Stardoc dependencies

# Begin test dependencies

load("//:defs.bzl", "maven_install")
load("//:specs.bzl", "maven")

maven_install(
    artifacts = [
        "androidx.test.espresso:espresso-core:3.1.1",
        "androidx.test.espresso:espresso-web:3.1.1",
        "androidx.test.ext:junit:1.1.0",
        "androidx.test:runner:1.1.1",
        "com.android.support:design:28.0.0",
        "com.google.android.gms:play-services-maps:16.0.0",
        "com.google.code.gson:gson:2.8.5",
        "com.google.guava:guava:27.0-android",
        "com.google.inject:guice:4.0",
        "commons-io:commons-io:2.6",
        "io.reactivex.rxjava2:rxjava:2.2.4",
        "junit:junit:4.12",
        "org.hamcrest:java-hamcrest:2.0.0.0",
        "org.mockito:mockito-core:2.22.0",
        "org.springframework:spring-core:5.1.3.RELEASE",
    ],
    fetch_sources = True,
    repositories = [
        "https://jcenter.bintray.com/",
        "https://maven.google.com",
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "other_maven",
    artifacts = [
        "com.google.guava:guava:27.0-jre",
    ],
    fetch_sources = True,
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "other_maven_with_exclusions",
    artifacts = [
        maven.artifact(
            group = "com.google.guava",
            artifact = "guava",
            version = "27.0-jre",
            exclusions = [
                maven.exclusion(
                    group = "org.codehaus.mojo",
                    artifact = "animal-sniffer-annotations",
                ),
                "com.google.j2objc:j2objc-annotations",
            ],
        ),
    ],
    fetch_sources = True,
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)

maven_install(
    name = "unsafe_shared_cache",
    artifacts = [
        "com.google.guava:guava:27.0-jre",
    ],
    fetch_sources = True,
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
    use_unsafe_shared_cache = True,
)

BAZEL_SKYLIB_TAG = "0.6.0"

http_archive(
    name = "bazel_skylib_local",
    strip_prefix = "bazel-skylib-%s" % BAZEL_SKYLIB_TAG,
    url = "https://github.com/bazelbuild/bazel-skylib/archive/%s.tar.gz" % BAZEL_SKYLIB_TAG,
)

# End test dependencies
