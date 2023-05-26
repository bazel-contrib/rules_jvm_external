java_import(
    name = "jetifier_jars",
    jars = glob(["jetifier-standalone/lib/*.jar"]),
)

java_import(
    name = "all_android_tools",
    jars = ["@android_tools//:all_android_tools_deploy.jar"],
)

java_library(
    name = "jetifier_worker_wrapper",
    visibility = ["//visibility:public"],
    srcs = glob(["**/*.java"]),
    deps = [
        "//third_party/jetifier:jetifier_jars",
        ":all_android_tools",
    ],
)

java_binary(
    name = "jetifier",
    main_class = "com.github.bazelbuild.rules_jvm_external.JetifierWorkerWrapper",
    visibility = ["//visibility:public"],
    runtime_deps = [
        ":jetifier_jars",
        ":jetifier_worker_wrapper"
    ],
)
