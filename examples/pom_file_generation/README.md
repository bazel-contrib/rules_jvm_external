# Generating <dependency> nodes for a POM file

This example shows how you can generate the `<dependencies>` section of a POM
file using `rules_jvm_external`'s generated targets and the `pom_file` rule from
`bazel-common`.

For reference,
[this](http://central.maven.org/maven2/com/google/inject/guice/4.0/guice-4.0.pom)
is the actual POM file for Guice 4.0.

To generate the XML, build the `//:guice_pom` target:

```
$ bazel build //:guice_pom
Starting local Bazel server and connecting to it...
INFO: Analysed target //:guice_pom (13 packages loaded, 427 targets configured).
INFO: Found 1 target...
Target //:guice_pom up-to-date:
  bazel-bin/guice_pom.xml
INFO: Elapsed time: 8.183s, Critical Path: 0.01s
INFO: 0 processes.
INFO: Build completed successfully, 2 total actions
```

Comparing to the actual POM file for Guice, we can see that the `pom_file` rule
successfully generated three `<dependency>` declarations for Guice's direct
non-test and non-optional dependencies.

```
$ cat bazel-bin/guice_pom.xml 
```

```xml
<dependency>
  <groupId>aopalliance</groupId>
  <artifactId>aopalliance</artifactId>
  <version>1.0</version>
</dependency>
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>16.0.1</version>
</dependency>
<dependency>
  <groupId>javax.inject</groupId>
  <artifactId>javax.inject</artifactId>
  <version>1</version>
</dependency>
```

Same goes for the `//:my_binary` Java target, which depends on `guice` and
`guava`. Building `//:my_binary_pom` generates `bazel-bin/my_binary_pom.xml`
containing these expected declarations:

```
$ cat bazel-bin/my_binary_pom.xml 
```

```xml
<dependency>
  <groupId>com.google.guava</groupId>
  <artifactId>guava</artifactId>
  <version>27.1-jre</version>
</dependency>
<dependency>
  <groupId>com.google.inject</groupId>
  <artifactId>guice</artifactId>
  <version>4.0</version>
</dependency>
```
