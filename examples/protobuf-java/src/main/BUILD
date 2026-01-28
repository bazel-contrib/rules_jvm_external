load("@com_google_protobuf//bazel:java_proto_library.bzl", "java_proto_library")
load("@rules_java//java:java_binary.bzl", "java_binary")
load("@rules_proto//proto:defs.bzl", "proto_library")

java_binary(
    name = "example",
    srcs = ["Main.java"],
    main_class = "rules.jvm.external.Main",
    visibility = ["//src/test:__subpackages__"],
    deps = [
        ":addressbook_proto_java",
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
    ],
)

proto_library(
    name = "addressbook_proto",
    srcs = ["addressbook.proto"],
)

java_proto_library(
    name = "addressbook_proto_java",
    deps = [":addressbook_proto"],
)
