load("@rules_jvm_external//:mvn.bzl", "mvn_package")

mvn_package(
    name = "hello_maven",
    srcs = [
        "//src/main/java/hello:srcs",
        # "//src/test/java/hello:srcs",
    ],
    pom_xml = "//:pom.xml",
    group_id = "org.springframework",
    artifact_id = "gs-spring-boot",
    version = "0.1.0",
)

java_binary(
    name = "hello_maven_app",
    runtime_deps = [":hello_maven"],
    main_class = "hello.Application",
)