# Spring Boot Example

This is an example of the tutorial app from [Spring's
website](https://spring.io/guides/gs/spring-boot/).

To build the Spring Boot application:

```
$ bazel build //src/main/java/hello:app
```

To run the Spring Boot application:

```
$ bazel run //src/main/java/hello:app
```

To run the tests:

```
$ bazel test //src/test/...
```

This tutorial code is [licensed under Apache 2.0, copyright GoPivotal
Inc.](https://github.com/spring-guides/gs-spring-boot/blob/d0f3a942f1b31ee73d2896c1e201f11cd8efd6ba/LICENSE.code.txt)
