package com.github.bazelbuild.rules_jvm_external.resolver;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.events.EventListener;
import com.github.bazelbuild.rules_jvm_external.resolver.maven.MavenResolver;
import com.github.bazelbuild.rules_jvm_external.resolver.netrc.Netrc;
import java.nio.file.Path;
import org.junit.Test;

public class MavenResolverTest extends ResolverTestBase {

  @Override
  protected Resolver getResolver(Netrc netrc, EventListener listener) {
    return new MavenResolver(netrc, listener);
  }

  @Test
  public void shouldSuccessfullyResolveNettyStaticClasses() {
    Coordinates main = new Coordinates("com.example:root:1.0.0");
    Coordinates x86Dep = new Coordinates("com.example", "root", null, "linux-x86_64", "1.0.0");

    Path repo = MavenRepo.create().add(main, x86Dep).getPath();

    // There should be no cycle detected by this dependency
    resolver.resolve(prepareRequestFor(repo.toUri(), main));
  }
}
