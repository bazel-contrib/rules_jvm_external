load("@bazel_skylib//rules:build_test.bzl", "build_test")

# This test checks if the testonly attribute was correctly applied.
# If testonly=True applied, this target should have testonly=1.
# But build_test doesn't check attribute values directly.
# We can use a rule that requires testonly dependencies?
# or just inspect with genquery.

# 1. Guava: Unset via chaining (1 -> 0). Expect: False (count 0)
genquery(
    name = "check_guava_testonly",
    expression = "attr(testonly, 1, @amend_artifacts//:com_google_guava_guava)",
    scope = ["@amend_artifacts//:com_google_guava_guava"],
)

# 2. Commons-Lang3: Set True via Int (1). Expect: True (count 1)
genquery(
    name = "check_lang3_testonly",
    expression = "attr(testonly, 1, @amend_artifacts//:org_apache_commons_commons_lang3)",
    scope = ["@amend_artifacts//:org_apache_commons_commons_lang3"],
)

# 3. Commons-Io: Set True via Legacy (True). Expect: True (count 1)
genquery(
    name = "check_io_testonly",
    expression = "attr(testonly, 1, @amend_artifacts//:commons_io_commons_io)",
    scope = ["@amend_artifacts//:commons_io_commons_io"],
)

# 4. Slf4j-api: Set Neverlink True via Int (1). Expect: True (count 1)
genquery(
    name = "check_slf4j_neverlink",
    expression = "attr(neverlink, 1, @amend_artifacts//:org_slf4j_slf4j_api)",
    scope = ["@amend_artifacts//:org_slf4j_slf4j_api"],
)

sh_test(
    name = "verify_testonly",
    srcs = ["verify.sh"],
    args = [
        "$(location :check_guava_testonly)",
        "$(location :check_lang3_testonly)",
        "$(location :check_io_testonly)",
        "$(location :check_slf4j_neverlink)",
    ],
    data = [
        ":check_guava_testonly",
        ":check_io_testonly",
        ":check_lang3_testonly",
        ":check_slf4j_neverlink",
    ],
)
