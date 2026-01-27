load("@rules_pkg//pkg:zip.bzl", "pkg_zip")

def create_jar(name, inputs, out = None, **kwargs):
    """Creates a JAR file from the given inputs.

    This macro uses rules_pkg's pkg_zip to create a JAR file (which is just a ZIP
    file with a .jar extension) from the provided input files.

    Args:
        name: The name of the target.
        inputs: A list of files or labels to include in the JAR.
        out: Optional output file name. Defaults to "{name}.jar".
        **kwargs: Additional arguments passed to pkg_zip.
    """
    if out == None:
        out = name + ".jar"
    elif not out.endswith(".jar"):
        out = out + ".jar"

    pkg_zip(
        name = name,
        srcs = inputs,
        out = out,
        **kwargs
    )
