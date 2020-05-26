MavenVersionProvider = provider(fields = ['maven_version'])

def _maven_version_impl(ctx):
    return MavenVersionProvider(maven_version = ctx.build_setting_value)

maven_version = rule(
    implementation = _maven_version_impl,
    build_setting = config.string(flag = True)
)
