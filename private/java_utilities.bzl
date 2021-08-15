#
# Utilites for working with java versions
#

# Get numeric java version from `java -version` output e.g:
#
#     openjdk version "11.0.9" 2020-10-20
#     OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.9+11)
#     OpenJDK 64-Bit Server VM AdoptOpenJDK (build 11.0.9+11, mixed mode)
#
def parse_java_version(java_version_output):
    # Look for 'version "x.y.z"' in the output'
    if len(java_version_output.split("version ")) > 1:
        java_version = java_version_output.split("version ")[1].partition("\n")[0].split(" ")[0].replace("\"", "")
    else:
        return None

    if not java_version:
        return None
    elif "." not in java_version:
        return int(java_version)
    elif java_version.startswith("1."):
        return int(java_version.split(".")[1])
    else:
        return int(java_version.split(".")[0])

    return None
