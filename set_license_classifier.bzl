

def set_default_license_classifier_impl(rctx):
  print("Doing that shit")
  rctx.file("license_classifier.bzl", content = """
def lookup_license(url=None, sha256=None, maven_id=None):
  return None
""")
  rctx.file("BUILD", content = "")

set_default_license_classifier = repository_rule(
    implementation=set_default_license_classifier_impl,
)

def use_default_license_classifier():
    if not native.existing_rule("rules_jvm_license_classifier"):
        set_default_license_classifier(
            name = "rules_jvm_license_classifier",
        )

def set_license_classifier(path):
    if native.existing_rule("rules_jvm_license_classifier"):
        fail("You are trying to set the rules_jvm_external license classifier a second time.")
    native.local_repository(
        name = "rules_jvm_license_classifier",
        path = path)
