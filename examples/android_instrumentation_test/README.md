This is a barebones Android app obtained from the Bazel Android tutorial.

- Test (Linux only): `$ bazel test //src/test:greeter_test`
- Build (All OSes): `$ bazel build //src/test:greeter_test_app //src/main:greeter_app`

See the [`//src/test/java/com/example/bazel:test_deps`](https://github.com/jin/rules_coursier_prototype/blob/77be93cd1f11b1cf627bc55e9460b955d5fa2f50/examples/android_instrumentation_test/src/test/java/com/example/bazel/BUILD#L16) target for the `artifact()` macro usage.
