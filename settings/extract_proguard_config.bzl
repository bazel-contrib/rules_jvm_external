ExtractProguardConfigProvider = provider(fields = ["extract_proguard_config"])

def _impl(ctx):
    return ExtractProguardConfigProvider(extract_proguard_config = ctx.build_setting_value)

extract_proguard_config = rule(
    implementation = _impl,
    build_setting = config.bool(flag = True),
)
