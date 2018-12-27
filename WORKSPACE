workspace(name = "rules_maven")
android_sdk_repository(
    name = 'androidsdk',
)

local_repository(
    name = 'gmaven_rules',
    path = '.',
)
load('@gmaven_rules//:gmaven.bzl', 'gmaven_rules')
gmaven_rules()

local_repository(
    name = "rules_maven",
    path = "/usr/local/google/home/jingwen/code/coursier/rules_maven/",
)

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Begin test dependencies

load("//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "androidx.test.espresso:espresso-core:3.1.1",
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
        "https://bintray.com/bintray/jcenter",
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

android_sdk_repository(name = "androidsdk")

BAZEL_SKYLIB_TAG = "0.6.0"

http_archive(
    name = "bazel_skylib",
    strip_prefix = "bazel-skylib-%s" % BAZEL_SKYLIB_TAG,
    url = "https://github.com/bazelbuild/bazel-skylib/archive/%s.tar.gz" % BAZEL_SKYLIB_TAG,
)

# load("@bazel_skylib//:workspace.bzl", "bazel_skylib_workspace")
# bazel_skylib_workspace()

# End test dependencies
