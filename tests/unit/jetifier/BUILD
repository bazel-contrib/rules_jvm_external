load("@bazel_skylib//rules:build_test.bzl", "build_test")
load(":jetifier_test.bzl", "jetifier_test_suite")

jetifier_test_suite()

build_test(
    name = "jetify_all_artifacts",
    targets = [
        "@jetify_all_test//:com_google_guava_guava",

        # Even though we probably shouldn't jetify android legacy support lib deps, this should only
        # happen when:
        # We jetify everything. so nothing should be depending on these support libs anyways;
        # We explicitly include the support lib to jetify, where you know what you're doing. Maybe.
        "@jetify_all_test//:com_android_support_appcompat_v7",
        "@jetify_all_test//:com_android_support_appcompat_v7_28_0_0",
        "@jetify_all_test//:jetified_com_android_support_appcompat_v7",
        "@jetify_all_test//:androidx_appcompat_appcompat",
        "@jetify_all_test//:androidx_appcompat_appcompat_1_0_0",
        "@jetify_all_test//:com_android_support_swiperefreshlayout",
        "@jetify_all_test//:com_android_support_swiperefreshlayout_28_0_0",
        "@jetify_all_test//:jetified_com_android_support_swiperefreshlayout",
        "@jetify_all_test//:androidx_swiperefreshlayout_swiperefreshlayout",
        "@jetify_all_test//:androidx_swiperefreshlayout_swiperefreshlayout_1_0_0",
    ],
)

genrule(
    name = "jetify_all_support_library_classes",
    srcs = ["@jetify_all_test//:jetified_com_android_support_appcompat_v7"],
    outs = ["jetify_all_jetified_classes.jar"],
    cmd = "unzip -p $< classes.jar > $@",
)

sh_test(
    name = "test_jetify_all_classes_jar",
    size = "small",
    srcs = ["jetify_all_test.sh"],
    data = [":jetify_all_jetified_classes.jar"],
    deps = ["@bazel_tools//tools/bash/runfiles"],
)

build_test(
    name = "jetify_include_list_artifacts",
    targets = [
        "@jetify_include_list_test//:com_google_guava_guava",
        "@jetify_include_list_test//:com_android_support_appcompat_v7",
        "@jetify_include_list_test//:com_android_support_appcompat_v7_28_0_0",
        "@jetify_include_list_test//:jetified_com_android_support_appcompat_v7",
        "@jetify_include_list_test//:androidx_appcompat_appcompat",
        "@jetify_include_list_test//:androidx_appcompat_appcompat_1_0_0",

        # We expect both "support" and "androidx" versions of annotations artifact to be present after jetification.
        "@jetify_include_list_test//:com_android_support_support_annotations_28_0_0",
        "@jetify_include_list_test//:androidx_annotation_annotation_1_0_0",
        "@jetify_include_list_test//:androidx_annotation_annotation",
        "@jetify_include_list_test//:com_android_support_support_annotations",

        # We don't expect jetified_ version of swiperefreshlayout.
        "@jetify_include_list_test//:com_android_support_swiperefreshlayout",
        "@jetify_include_list_test//:com_android_support_swiperefreshlayout_28_0_0",
        "@jetify_include_list_test//:androidx_swiperefreshlayout_swiperefreshlayout",
        "@jetify_include_list_test//:androidx_swiperefreshlayout_swiperefreshlayout_1_0_0",
    ],
)

genrule(
    name = "jetify_include_list_classes",
    srcs = [
        "@jetify_include_list_test//:jetified_com_android_support_appcompat_v7",
        "@jetify_include_list_test//:com_android_support_swiperefreshlayout",
    ],
    outs = ["jetify_include_list_classes.txt"],
    cmd = """
    for SRC in $(SRCS)
    do
        if [[ $$SRC == *.aar ]]; then
            mkdir aar
            unzip -q -d aar $$SRC
            if [[ -f aar/classes.jar ]]; then
                jar tf aar/classes.jar >> $@;
            fi
            rm -rf aar
        elif [[ $$SRC == *.jar ]]; then
            jar tf $$SRC >> $@
        fi
    done
    """,
)

sh_test(
    name = "test_jetify_include_list_classes_jar",
    size = "small",
    srcs = ["jetify_include_list_test.sh"],
    data = [":jetify_include_list_classes.txt"],
    deps = ["@bazel_tools//tools/bash/runfiles"],
)
