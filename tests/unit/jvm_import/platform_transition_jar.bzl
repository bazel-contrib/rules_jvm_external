def _xsitn_impl(settings, attr):
    return {"//command_line_option:platforms": str(attr.platform)}

_transition = transition(
    implementation = _xsitn_impl,
    inputs = [],
    outputs = ["//command_line_option:platforms"],
)

def _rule_impl(ctx):
    return [
        DefaultInfo(files = depset(transitive = [ctx.attr.src[0][DefaultInfo].files])),
    ]

platform_transition_jar = rule(
    implementation = _rule_impl,
    attrs = {
        "src": attr.label(
            cfg = _transition,
        ),
        "platform": attr.label(),
    },
    doc = """
        Depend on a JAR for a specified platform.
        This isn't typical usage - for the most part, Java builds should use a consistent config.
        Using a transition enables modelling multi-platform builds within the build system for the purposes of testing.
    """,
)
