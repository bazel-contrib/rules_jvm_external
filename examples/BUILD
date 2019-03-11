load("@rules_jvm_external//:defs.bzl", "gmaven_artifact")

android_binary(
    name = 'app',
    manifest = 'AndroidManifest.xml',
    custom_package = 'com.example.foo',
    multidex = 'native',
    deps = [
        '@com_android_support_design_27_0_1//aar',
        gmaven_artifact("com.google.android.gms:play-services:aar:12.0.0"),
    ],
)
