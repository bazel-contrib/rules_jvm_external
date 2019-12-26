java_import(
    name = "jetifier_jars",
    jars = glob(["jetifier-standalone/lib/*.jar"]),
)

java_binary(
    name = "jetifier",
    main_class = "com.android.tools.build.jetifier.standalone.Main",
    runtime_deps = [
        ":jetifier_jars"
    ],
    visibility = ["//visibility:public"],
)
