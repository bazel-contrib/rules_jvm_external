# Spring Boot Example

This is an example of the tutorial app from [Spring's website](https://spring.io/guides/gs/spring-boot/).

To build the Spring Boot application:

```
$ bazel build //src/main/java/hello:app
```

To run the Spring Boot application:

```
$ bazel run //src/main/java/hello:app
```

To run the test:

```
$ bazel test //src/test/java/hello:test
```
