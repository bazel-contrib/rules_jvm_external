

CONTENT = """
def lookup_license(url=None, sha256=None, maven_id=None):
  return None
"""

def set_license_classifier_impl(rctx):
  rctx.file("license_classifier.bzl", content = CONTENT)
  rctx.file("BUILD", content = "")


set_default_license_classifier = repository_rule(
    implementation=set_license_classifier_impl,
    # local=True,
    attrs={"path": attr.label(mandatory=True)},
)

def use_default_license_classifier():
    set_default_license_classifier(
        name = "rules_jvm_license_classifier",
        path = Label("//null_classifier"),
    )


def set_license_classifier(path):
    if not native.existing_rule("rules_jvm_license_classifier"):
        if path:
            native.local_repository(
                name = "rules_jvm_license_classifier",
                path = path)

