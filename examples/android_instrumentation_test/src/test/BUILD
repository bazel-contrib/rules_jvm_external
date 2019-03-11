android_binary(
    name = "greeter_test_app",
    custom_package = "com.example.bazel.test",
    instruments = "//src/main:greeter_app",
    manifest = "AndroidManifest.xml",
    deps = ["//src/test/java/com/example/bazel:greeter_test_lib"],
)

android_instrumentation_test(
    name = "greeter_test",
    target_device = "@android_test_support//tools/android/emulated_devices/generic_phone:android_23_x86_qemu2",
    test_app = ":greeter_test_app",
)
