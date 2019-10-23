java_binary(
    name = "example",
    srcs = [ "Main.java" ],
    main_class = "rules.jvm.external.Main",
    deps = [
        "@maven//:com_google_protobuf_protobuf_java",
        "@maven//:com_google_protobuf_protobuf_java_util",
        ":addressbook_proto_java",
    ],
    visibility = ["//src/test:__subpackages__"],
)

proto_library(
    name = "addressbook_proto",
    srcs = ["addressbook.proto"],
)

java_proto_library(
    name = "addressbook_proto_java",
    deps = [":addressbook_proto"],
)
