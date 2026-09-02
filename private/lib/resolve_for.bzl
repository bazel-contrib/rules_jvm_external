def resolve_for_error(resolver, resolve_for):
    if resolve_for not in ["jvm", "android"]:
        return "resolve_for must be either \"jvm\" or \"android\", got \"%s\"" % resolve_for
    if resolve_for == "android" and resolver != "gradle":
        return "resolve_for = \"android\" requires resolver = \"gradle\""
    return None

def validate_resolve_for(resolver, resolve_for):
    error = resolve_for_error(resolver, resolve_for)
    if error:
        fail(error)
