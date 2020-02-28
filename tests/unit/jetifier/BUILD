load("@bazel_skylib//rules:build_test.bzl", "build_test")

build_test(
    name = "jetify_all_artifacts",
    targets = [
        "@jetify_all_test//:com_google_guava_guava",
        "@jetify_all_test//:com_android_support_appcompat_v7",
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
        "@jetify_include_list_test//:com_android_support_support_annotations_28_0_0",
        "@jetify_include_list_test//:androidx_annotation_annotation_1_0_0",
        "@jetify_include_list_test//:androidx_annotation_annotation",
        "@jetify_include_list_test//:com_android_support_support_annotations",
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
