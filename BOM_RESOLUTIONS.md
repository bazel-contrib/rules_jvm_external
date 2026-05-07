# BOM Resolutions Specification

## Inputs

### User Configuration
- **`store_bom_resolution = True/False`** in `maven.install()` tag - Controls whether BOM resolution information is computed and stored. Default `False`.
- **BOMs declared in `boms = [...]`** - List of BOM coordinates that manage dependency versions.
- **Versionless artifacts in `artifacts = [...]`** - Dependencies without explicit versions that BOMs may manage. This includes entries from `maven.artifact()` that omit `version`.
- **Repositories in `repositories = [...]`** - Maven repositories where BOMs can be resolved.

### Example Usage
```python
maven.install(
    artifacts = [
        "com.google.auth:google-auth-library-oauth2-http",  # versionless
        "ch.qos.logback:logback-classic",                   # versionless
    ],
    boms = [
        "com.google.cloud:libraries-bom:26.59.0",
        "org.springframework.boot:spring-boot-dependencies:3.5.14",
    ],
    store_bom_resolution = True,  # Enable BOM resolution tracking
)
```

### Multi-Module Merging of `store_bom_resolution`
When multiple `install` tags with the same `name` are merged across modules, the
flag is combined with logical-OR: if **any** module sets `store_bom_resolution =
True`, the merged install gets `True`. Rationale: the feature is purely additive
(it only writes more data into the lock file), so a leaf module wanting BOM
tracking should not be silently overridden by the root module's default.

This differs from the default "root module wins" merge precedence used for most
other install-tag fields and must be implemented explicitly in `maven.bzl`.

## Outputs

### Lock File Structure
BOM resolution information is stored as a `bom_resolution` section in v3 lock files:

```json
{
  "artifacts": { ... },
  "dependencies": { ... },
  "repositories": { ... },
  "version": "3",
  "bom_resolution": {
    "com.google.auth:google-auth-library-oauth2-http": [
      "com.google.cloud:libraries-bom:26.59.0"
    ],
    "ch.qos.logback:logback-classic": [
      "org.springframework.boot:spring-boot-dependencies:3.5.14"
    ]
  }
}
```

#### Key Format
Map keys identifying the managed artifact follow this rule:
- **Default packaging (`jar`) and no classifier:** `group:artifact`
- **Otherwise:** full Maven coordinates without version, in the canonical order
  `group:artifact:packaging[:classifier]`

This collapses the common case to the simplest representation while still
distinguishing variants (e.g. `aar` packaging or a `linux-x86_64` classifier)
that resolve to genuinely different artifacts.

#### Value Format
Values are **ordered, deduplicated** lists of declared BOM coordinates that
manage the artifact:
- Order is the BOM declaration order (root-module declarations first; see
  Constraints).
- Each directly-declared BOM appears at most once even if it is reachable via
  multiple paths (e.g. directly declared *and* imported transitively by another
  declared BOM).
- Only **directly-declared** BOMs are listed. Sub-BOMs imported transitively by
  a declared BOM are not surfaced as their own entries; their managed artifacts
  are attributed to the directly-declared parent BOM.

### Generated BUILD Tags
Artifacts managed by BOMs get one `maven_bom_coordinate=<bom>` tag per managing
BOM in the generated `jvm_import` rules, in declaration order:

```python
jvm_import(
    name = "com_google_auth_google_auth_library_oauth2_http",
    tags = [
        "maven_coordinates=com.google.auth:google-auth-library-oauth2-http:1.23.0",
        "maven_bom_coordinate=com.google.cloud:libraries-bom:26.59.0",
        # If a second BOM also manages this artifact, it appears as a second tag:
        # "maven_bom_coordinate=org.springframework.boot:spring-boot-dependencies:3.5.14",
    ],
    # ...
)
```

Multiple managing BOMs surface as multiple separate tags (not comma-joined);
downstream tools iterate tags normally.

### Input Hash Impact
- `bom_resolution_enabled` marker is added to `__INPUT_ARTIFACTS_HASH` **only
  when the feature is enabled**. When `store_bom_resolution = False` (the
  default), the key is absent from `__INPUT_ARTIFACTS_HASH`, preserving the
  exact hash existing users have today and avoiding lock-file churn for
  non-adopters.
- Toggling `store_bom_resolution` between `True` and `False` therefore changes
  the hash and triggers lock file regeneration; toggling it from absent to
  `False` does not.
- The `bom_resolution` section itself does not affect resolved artifact hashes
  (it is annotation-only).
- Other hash values are unaffected by this change.

## Constraints

### Workflow Decisions
1. **Post-Pin Processing**: BOM resolution runs AFTER dependency resolution,
   not during. This is uniform across both resolvers — the Maven (Aether)
   resolver does **not** piggyback on its own Aether session; it invokes the
   same standalone `BomResolverMain` post-pin pass as Coursier. Uniformity
   simplifies testing and reasoning at the cost of one extra Aether session
   during pin (acceptable for a rare operation).
2. **Both Resolvers Supported**: Works with both Maven resolver and Coursier
   resolver via the same `BomResolverMain` invocation.
3. **Versionless Only**: Only tracks artifacts that were requested without an
   explicit version. Artifacts with an explicit version (whether in shorthand
   `g:a:v` form or `maven.artifact(..., version=...)`) are **silently skipped**
   even if a declared BOM also manages them — no annotation, no warning. The
   user's explicit version wins; respecting that intent without nagging is the
   policy.
4. **Declaration Order with Dedup**: Multiple BOMs managing the same artifact
   are listed in declaration order, with the following dedup rules:
   - **Same `group:artifact`, different versions** across merged install tags:
     dedupe by `group:artifact`; the **first-declared** version wins
     (root-module declarations come first per existing merge precedence).
   - **Exact duplicates**: dedupe silently.
   - **Same direct BOM reachable via multiple paths** (e.g. directly declared
     *and* imported transitively by another directly-declared BOM): listed once
     in its directly-declared position.
5. **User-Visible BOMs Only**: Records directly-declared BOMs, not internal
   sub-BOMs imported transitively. A BOM that is both directly declared **and**
   reachable transitively still appears exactly once (in its declaration
   position).
6. **Optional Feature**: Default `store_bom_resolution = False` for backward
   compatibility.
7. **Skipped Artifact Categories**: The following are excluded from
   `bom_resolution` even if a declared BOM would manage them, each for its own
   reason:
   - **`excluded_artifacts`**: not in the resolved tree; no `jvm_import` exists
     to annotate. Recording them would falsely imply the BOM affects this
     build.
   - **`override_targets`**: the `jvm_import` is replaced by a user-supplied
     label (often local). The BOM coords no longer correspond to what is built
     and the rule does not generate the override target.
   - **Explicitly version-pinned artifacts** (covered by Constraint #3, listed
     here for completeness).

### Technical Constraints
8. **No Prebuilt JAR Impact**: Uses a separate `BomResolverMain` command/JAR to
   avoid modifying `lock_file_converter_deploy.jar`.
9. **Network Required**: BOM resolution requires downloading and parsing BOM
   POMs from repositories.
10. **Aether-Based**: Uses Maven Aether for consistent BOM resolution across
    resolvers.
11. **Hard Fail on Resolution Errors**: If `BomResolverMain` cannot resolve a
    declared BOM (network error, 404, malformed POM, auth failure), the pin
    command **aborts with a clear error message**. No partial / best-effort
    `bom_resolution` is written. Matches the rest of the resolver's
    "fail-fast on pin" behavior and prevents silent drift between lock file
    and reality.
12. **v3 Lock File Required**: `store_bom_resolution = True` requires the v3
    lock file format. If the existing lock file is v2 or older, the pin
    command errors with a clear message instructing the user to regenerate
    the lock file as v3. The flag is not silently ignored.
13. **Empty / `{}` Lock File Tolerance (Cross-Cutting)**: All v3 lock-file
    parsers — `V3LockFile.java`, `LockFileConverter.java`, and any Starlark
    consumers in `v3_lock_file.bzl` and `pin_dependencies.bzl` — must
    gracefully accept empty files and `{}` as "no data; regenerate". This is
    a general v3 parser requirement (not BOM-specific) so that
    `echo '{}' > <lock-file>` is a valid reset mechanism for tests and users.
14. **Repository Credentials**: BOM resolution reuses whatever credential
    plumbing the existing resolver already uses (netrc paths, URL-embedded
    credentials, env vars). No new user-facing attribute is introduced;
    `pin.sh` forwards the existing values to `BomResolverMain` via `--netrc=`
    and standard URLs.

## Architecture

### `BomResolverMain` CLI Contract
`BomResolverMain` is a standalone Java entry point invoked by `pin.sh`. The
list of BOMs, repositories, and versionless artifacts is **not** stored in the
lock file, so all of these inputs are passed on the command line.

```
BomResolverMain \
    --lock-file=<path>          (required; edited in place) \
    --boms=<coord>              (repeatable; declaration order preserved) \
    --repositories=<url>        (repeatable; declaration order preserved) \
    --artifacts=<coord>         (repeatable; the versionless requested set,
                                 already filtered to remove excluded /
                                 overridden / explicitly version-pinned
                                 entries before invocation) \
    --netrc=<path>              (optional; mirrors existing resolver plumbing)
```

Behavior:
- Reads the existing lock file at `--lock-file`.
- Computes BOM resolution mappings using Aether against `--repositories`,
  `--boms`, and `--artifacts`.
- Adds or replaces the `bom_resolution` section in the lock file.
- Preserves all other lock file content byte-for-byte where possible (uses
  `Gson` with stable formatting).
- Writes the result back to `--lock-file` in place.
- Exit code `0` on success; non-zero with a descriptive `stderr` message on
  any resolution failure (Constraint #11).

Note on argv length: typical installs (tens of artifacts, single-digit BOMs)
fit comfortably within OS argv limits (Linux ~2 MB total, macOS ~1 MB).
Degenerate cases with thousands of versionless artifacts can be addressed
later by adding `--artifacts-file=` etc. without breaking compatibility.

### Classes and Interfaces

#### Core Resolution Logic
- **`BomResolver`** - Static utility class with core BOM resolution algorithm
  - `buildBomResolutionMapping(repositories, boms, versionlessArtifacts, netrcFile)` → `Map<String, List<String>>`
  - `getEffectiveManagedDependencies(system, session, repositories, bomCoords)` → `Set<String>`
- **`BomResolverMain`** - CLI entry point for the standalone BOM resolution tool
  - Reads existing lock file, adds/updates `bom_resolution` section, writes back
  - Preserves all existing lock file content using `Gson`

#### Integration Points
- **`maven.bzl`** - Bazel module extension for Maven resolver integration
  - Propagates `store_bom_resolution` from `install` tag to repository rules
    using logical-OR merge across modules (see "Multi-Module Merging" above)
  - Handles empty / minimal JSON lock files during parsing (Constraint #13)
- **`coursier.bzl`** - Coursier resolver integration
  - Adds `store_bom_resolution` attribute to `coursier_fetch` and
    `pinned_coursier_fetch`
  - Includes `bom_resolution_enabled` marker in input signature computation
    only when enabled (asymmetric; see "Input Hash Impact")
  - Templates `pin.sh` to invoke `BomResolverMain` with the CLI contract above
    when enabled
  - On `pinned_coursier_fetch` (non-repin builds), **validates** consistency
    between the attribute and the lock file:
    - Attribute `True` and `bom_resolution` section missing → **error**
      (suggests stale lock file; user should repin)
    - Attribute `False` and `bom_resolution` section present → **warn**
      (stale stored data, harmless but worth flagging)
    - Otherwise → consume `bom_resolution` from the lock file as-is
- **`dependency_tree_parser.bzl`** - BUILD file generation
  - Accepts `bom_resolution` data and emits one `maven_bom_coordinate=<bom>`
    tag per managing BOM, in declaration order

#### Data Structures
- **`V3LockFile`** - v3 lock file format with optional `bom_resolution` section
- **Lock File Hash Components**:
  - `__INPUT_ARTIFACTS_HASH.bom_resolution_enabled` - present **only when
    enabled**; absent otherwise (preserves backward-compat hashes)
  - `__RESOLVED_ARTIFACTS_HASH` - unchanged by BOM annotations

### Process Flow
1. **Pin Command**: `REPIN=1 bazel run @repo//:pin`
2. **Dependency Resolution**: Normal artifact resolution (Maven/Coursier)
3. **Lock File Generation**: Basic v3 lock file created
4. **BOM Resolution** (if `store_bom_resolution = True`): `pin.sh` invokes
   `BomResolverMain` with CLI flags carrying the BOMs / repositories /
   versionless-artifacts list (none of which are in the lock file). The tool
   edits the lock file in place to add the `bom_resolution` section.
5. **BUILD Generation**: `dependency_tree_parser.bzl` reads the final lock
   file with BOM data and emits `maven_bom_coordinate=` tags.

## Testing

### Unit Tests

#### Starlark Tests (`tests/unit/coursier_test.bzl`)
- **`bom_resolution_flag_changes_input_hash_test`** - Hash differs between enabled/disabled states
- **`bom_resolution_flag_marker_present_when_enabled_test`** - Exact hash value when enabled (asserts marker is present in `__INPUT_ARTIFACTS_HASH`)
- **`bom_resolution_flag_marker_absent_when_disabled_test`** - Exact hash value when disabled (asserts marker is **absent** from `__INPUT_ARTIFACTS_HASH`, matching the asymmetric encoding)
- **Anti-faking**: Must call `asserts.false(env, hash_off == hash_on)` - stubs returning the same value fail.

#### Java Tests (`tests/.../bomresolver/BomResolverTest.java`)
- **`testEmptyInputs`** - Empty BOMs or versionless artifacts return empty mapping
- **`testNoMatchingArtifacts`** - BOMs not managing requested artifacts return empty mapping
- **`testSingleBomManagesArtifact`** - Single BOM managing artifact (uses `org.junit:junit-bom:5.10.0`)
- **`testMultipleBomsSameArtifact`** - Multiple BOMs managing same artifact in declaration order
- **`testRecursiveImportedBoms`** - Parent BOM attribution, not sub-BOM (uses `com.google.cloud:libraries-bom`)
- **`testDirectAndTransitiveBomDeduped`** - A directly-declared BOM also imported by another declared BOM appears exactly once, in its declaration position
- **`testVersionedArtifactsIgnored`** - Artifacts with explicit versions are silently skipped, even if a BOM manages them
- **`testNonDefaultPackagingKeyFormat`** - Verifies key format rule: default packaging+no classifier ⇒ `g:a`; otherwise full coords
- **`testMultipleRepositories`** - Works with multiple Maven repositories
- **`testHardFailOnResolutionError`** - A bad BOM coord / unreachable repo causes a non-zero exit / thrown exception (no partial output)
- **Anti-faking**: `assertEquals(expected, actual)` - empty results fail explicitly.

### Integration Tests (`tests/bazel_run_tests.sh`)

#### Enabled Cases
- **`test_coursier_resolution_with_boms`** - Coursier resolver with BOM resolution
  - Reset: `echo '{}' > coursier_resolved_install.json`
  - Assert: `jq '.bom_resolution | length' >= 2` and specific mappings exist
- **`test_maven_resolution_with_boms`** - Maven resolver with BOM resolution
  - Reset: `echo '{}' > maven_resolved_install.json`
  - Assert: Selenium BOM mappings for `selenium-api` and `selenium-support`
- **`test_multiple_boms_emit_multiple_tags`** - An artifact managed by two declared BOMs produces two separate `maven_bom_coordinate=` tags in the generated BUILD output, in declaration order.

#### Disabled Cases
- **`test_coursier_resolution_without_bom_resolution`** - Feature disabled
  - Reset: `echo '{}' > coursier_resolved_without_bom_resolution_install.json`
  - Assert: `jq '. | has("bom_resolution") | not'`
  - Assert: `jq '.__INPUT_ARTIFACTS_HASH | has("bom_resolution_enabled") | not'` (asymmetric encoding — marker absent when disabled)

#### v2 Lock File Rejection
- **`test_v2_lock_file_with_bom_resolution_errors`** - Setting
  `store_bom_resolution = True` against an existing v2 lock file aborts the
  pin with a clear error message instructing the user to regenerate as v3.

#### Hard-Fail Behavior
- **`test_unresolvable_bom_aborts_pin`** - Declaring a non-existent BOM
  coordinate causes `REPIN=1 bazel run //:pin` to exit non-zero with a
  descriptive error; the lock file is not updated.

#### `pinned_coursier_fetch` Validation
- **`test_pinned_fetch_errors_when_attr_true_section_missing`** - Builds with
  `store_bom_resolution = True` against a lock file lacking the section fail
  with a "stale lock file" error.
- **`test_pinned_fetch_warns_when_attr_false_section_present`** - Builds with
  `store_bom_resolution = False` against a lock file containing the section
  emit a warning to stderr but succeed.

#### Generated Artifacts
- **`test_jvm_import_bom_tags`** - BUILD file tags validation
  - Assert: `grep -c 'maven_bom_coordinate=' >= 2` and both declared BOMs present

### Smoke Tests

#### End-to-End Validation
- **Pin Commands Work**: `REPIN=1 bazel run @coursier_resolved_with_boms//:pin` succeeds
- **Pin Commands Work**: `REPIN=1 bazel run @maven_resolved_with_boms//:pin` succeeds
- **Build Targets Work**: Generated `jvm_import` targets build successfully
- **Lock Files Valid**: Generated JSON passes schema validation
- **Empty Lock File Reset**: `echo '{}' > <lock-file> && bazel build //...` works
  for every v3-consuming code path (general Constraint #13 coverage).

### Anti-Faking Test Constraints

#### Mandatory Guardrails (Must Not Be Optional)
1. **Reset Before Assert**: `echo '{}' > <lock-file>` before pin command - prevents stale data false positives.
2. **Assert Exact Content**: Use `jq` for exact JSON matching, not `grep -q '"bom_resolution"'` - prevents empty result false positives.
3. **Assert Minimum Count**: `len(bom_resolution) >= N` for known N - prevents meaningless empty tests.
4. **Negative Case Verification**: When disabled, assert BOTH absence of `bom_resolution` section AND **absence** of `bom_resolution_enabled` from `__INPUT_ARTIFACTS_HASH` (asymmetric encoding).
5. **Hash Comparison Tests**: Assert `hash_enabled != hash_disabled` - prevents "do nothing" implementations.
6. **Real Test Execution**: Java tests as `java_test` targets, not `java_binary` - ensures CI actually runs them.
7. **Hard-Fail Not Silently Swallowed**: The unresolvable-BOM integration test must assert non-zero exit code AND that the lock file was not modified — prevents an implementation that swallows errors and writes an empty section.

#### Break-and-Revert Sanity Check
- Temporarily modify `BomResolver.buildBomResolutionMapping` to return `Collections.emptyMap()`.
- Confirm integration tests **fail loudly** with meaningful error messages.
- Revert change and confirm tests pass again.
- **Critical Rule**: If breaking the resolver doesn't break the tests, the tests aren't testing the resolver.

### Test Data Requirements
- **Stable External Dependencies**: Use well-known, stable BOMs like `org.junit:junit-bom:5.10.0`.
- **Network Access Acceptable**: Tests require Maven Central access (same as existing integration tests).
- **Deterministic Results**: Assert exact expected mappings, not just "some mapping exists".
- **Multiple Scenarios**: Test single BOM, multiple BOMs, recursive BOMs, direct-and-transitive dedup, non-default packaging, and no-BOM cases.
