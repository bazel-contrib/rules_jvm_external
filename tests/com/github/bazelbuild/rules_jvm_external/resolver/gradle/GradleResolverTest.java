package com.github.bazelbuild.rules_jvm_external.resolver.gradle;


import com.github.bazelbuild.rules_jvm_external.resolver.Resolver;
import com.github.bazelbuild.rules_jvm_external.resolver.ResolverTestBase;
import com.github.bazelbuild.rules_jvm_external.resolver.cmd.ResolverConfig;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;

public class GradleResolverTest extends ResolverTestBase {

    @Override
    protected Resolver getResolver(Netrc netrc, EventListener listener) {
        return new GradleResolver(netrc, ResolverConfig.DEFAULT_MAX_THREADS, listener);
    }
}