

def _run_binary_imp(ctx):
    tool_as_list = [ctx.attr.tool]
    args = [ctx.expand_location(a, tool_as_list) for a in ctx.attr.args]
    env = {k: ctx.expand_location(v, tool_as_list) 
      for k, v 
      in ctx.attr.env.items()
    }

    ctx.actions.run(
        executable = ctx.executable.tool,
        inputs = ctx.files.srcs,
        outputs = ctx.outputs.outs,
        arguments = args,
        env = env,
        mnemonic = "RunBinary",
        use_default_shell_env = ctx.attr.use_default_shell_env,
    )
    return DefaultInfo(
        files = depset(ctx.outputs.outs),        
    )


#run_binary: Runs a binary as a build action.
#  Note: Similar to Skylib's run_binary, but Skylib's implementation uses default_shell_env which makes it less hermetic on windows.
#  This implementation allows for more hermeticity by allowing user to set use_default_shell_env=False when they don't need the shell env.
#  (If/when skylib changes their implementation (skylib #567), this can probably be replaced with that one)
run_binary = rule(
    implementation = _run_binary_imp,
    attrs = {
        "tool" : attr.label(
            executable = True,
            allow_files = True,
            mandatory = True,
            cfg = "exec"
        ),
        "env": attr.string_dict(),
        "outs": attr.output_list(mandatory = True),
        "args": attr.string_list(),
        "srcs": attr.label_list(allow_files=True),
        "use_default_shell_env": attr.bool(default = False)
    }

)