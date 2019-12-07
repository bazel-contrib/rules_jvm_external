licenses(["notice"])

java_library(
    name = "lib",
    srcs = ["java/rules/jvm/external/Hasher.java"],
)

java_test(
    name = "test",
    srcs = ["java/rules/jvm/external/HasherTest.java"],
    test_class = "rules.jvm.external.HasherTest",
    deps = [
        ":lib",
        "@maven//:org_hamcrest_hamcrest",
    ],
)

java_binary(
    name = "hasher",
    main_class = "rules.jvm.external.Hasher",
    runtime_deps = [":lib"],
)

