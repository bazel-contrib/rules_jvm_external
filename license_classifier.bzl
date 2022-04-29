# lookup_license: Returns the license information for a maven it.
# The dependency parser calls this while generating the @maven/BUILD file.
# This is a no-op fallback, that will be overwritten by the patch command
# use_license_classifier.sh.

def lookup_license(url=None, sha256=None, maven_id=None):
    return None
