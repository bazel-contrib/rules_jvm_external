#
# Utilites for working with java versions
#

def is_version_character(c):
    """Returns True if the character is a version character, otherwise False."""
    return c.isdigit() or c == "."

def index_of_version_character(s):
    """Determines index of 1st occurrence of version character in string."""
    for i in range(len(s)):
        if is_version_character(s[i]):
            return i
    return None

def index_of_non_version_character_from(s, index):
    """Determines index of 1st occurrence of non-version character in string."""
    for i in range(index, len(s)):
        if not is_version_character(s[i]):
            return i
    return None

def get_major_version(java_version):
    """Returns the major version from Java version string."""
    if not java_version:
        return None
    elif "." not in java_version:
        return int(java_version)
    elif java_version.startswith("1."):
        return int(java_version.split(".")[1])
    else:
        return int(java_version.split(".")[0])

# Get numeric java version from `java -version` output e.g:
#
#     openjdk version "11.0.9" 2020-10-20
#     OpenJDK Runtime Environment AdoptOpenJDK (build 11.0.9+11)
#     OpenJDK 64-Bit Server VM AdoptOpenJDK (build 11.0.9+11, mixed mode)
#
def parse_java_version(java_version_output):
    first_line = java_version_output.strip().split("\n")[0]
    if not first_line:
        return None
    i = index_of_version_character(first_line)
    if i == None:
        return None
    j = index_of_non_version_character_from(first_line, i + 1)
    return get_major_version(first_line[i:j])

# Build the contents of a java Command-Line Argument File from a list of
# arguments.
#
# This quotes all arguments (and escapes all quotation marks in arguments) so
# that arguments containing white space are treated as single arguments.
def build_java_argsfile_content(args):
    return "\n".join(['"' + str(f).replace('"', r'\"') + '"' for f in args]) + "\n"
