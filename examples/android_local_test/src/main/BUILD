load("@rules_jvm_external//:defs.bzl", "artifact")

android_library(
     name = "my_lib",
     custom_package = "com.example.bazel",
     srcs = glob(["java/com/example/bazel/*.java"]),
     manifest = "java/AndroidManifest.xml",
     resource_files = glob(["res/**"]),
     deps = [
         artifact("androidx.appcompat:appcompat"),
         artifact("androidx.annotation:annotation"),
     ],
     visibility = ["//src/test:__subpackages__"]
)
