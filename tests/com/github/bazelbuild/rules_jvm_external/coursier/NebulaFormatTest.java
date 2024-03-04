package com.github.bazelbuild.rules_jvm_external.coursier;

import static org.junit.Assert.assertEquals;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.DependencyInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class NebulaFormatTest {

  private final Set<URI> repos = Set.of(URI.create("http://localhost/m2/repository"));

  @Test
  public void shouldRenderAggregatingJarsAsJarWithNullShasum() {
    DependencyInfo aggregator =
        new DependencyInfo(
            new Coordinates("com.example:aggregator:1.0.0"),
            repos,
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Set.of());

    Map<String, Object> rendered =
        new NebulaFormat()
            .render(
                repos.stream().map(Object::toString).collect(Collectors.toSet()),
                Set.of(aggregator),
                Map.of());

    Map<?, ?> artifacts = (Map<?, ?>) rendered.get("artifacts");
    Map<?, ?> data = (Map<?, ?>) artifacts.get("com.example:aggregator");
    Map<?, ?> shasums = (Map<?, ?>) data.get("shasums");

    HashMap<Object, Object> expected = new HashMap<>();
    expected.put("jar", null);
    assertEquals(expected, shasums);
  }
}
