# BOM Resolutions Specification

## Inputs

### User Configuration
- **`store_bom_resolution = True/False`** in `maven.install()` tag - Controls whether BOM resolution information is computed and stored
- **BOMs declared in `boms = [...]`** - List of BOM coordinates that manage dependency versions
- **Versionless artifacts in `artifacts = [...]`** - Dependencies without explicit versions that BOMs may manage. This includes entries from `maven.artifact()`.
- **Repositories in `repositories = [...]`** - Maven repositories where BOMs can be resolved

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

### Generated BUILD Tags
Artifacts managed by BOMs get `maven_bom_coordinate=<bom>` tags in generated `jvm_import` rules:

```python
jvm_import(
    name = "com_google_auth_google_auth_library_oauth2_http",
    tags = [
        "maven_coordinates=com.google.auth:google-auth-library-oauth2-http:1.23.0",
        "maven_bom_coordinate=com.google.cloud:libraries-bom:26.59.0",
    ],
    # ...
)
```

### Input Hash Impact
- `bom_resolution_enabled` marker is added to `__INPUT_ARTIFACTS_HASH` to track the feature state
- Changing `store_bom_resolution` triggers lock file regeneration
- The `bom_resolution` section itself does not affect resolved artifact hashes (annotation-only)
- Other hash values should not be impacted by this change

## Constraints

### Workflow Decisions
1. **Post-Pin Processing**: BOM resolution runs AFTER dependency resolution, not during
2. **Both Resolvers Supported**: Works with both Maven resolver and Coursier resolver
3. **Versionless Only**: Only tracks artifacts that were requested without explicit versions
4. **Declaration Order**: Multiple BOMs managing the same artifact are listed in declaration order
5. **User-Visible BOMs**: Records the top-level BOM declared by user, not internal sub-BOMs
6. **Optional Feature**: Default `store_bom_resolution = False` for backward compatibility

### Technical Constraints
7. **No Prebuilt JAR Impact**: Uses separate `BomResolverMain` command to avoid modifying `lock_file_converter_deploy.jar`
8. **Network Required**: BOM resolution requires downloading and parsing BOM POMs from repositories
9. **Aether-Based**: Uses Maven Aether for consistent BOM resolution across resolvers
10. **Empty File Handling**: Lock file parsers handle `{}` and empty files gracefully

## Architecture

### Classes and Interfaces

#### Core Resolution Logic
- **`BomResolver`** - Static utility class with core BOM resolution algorithm
  - `buildBomResolutionMapping(repositories, boms, versionlessArtifacts, netrcFile)` → `Map<String, List<String>>`
  - `getEffectiveManagedDependencies(system, session, repositories, bomCoords)` → `Set<String>`
- **`BomResolverMain`** - CLI entry point for standalone BOM resolution tool
  - Reads existing lock file, adds/updates `bom_resolution` section, writes back
  - Preserves all existing lock file content using `Gson`

#### Integration Points
- **`maven.bzl`** - Bazel module extension for Maven resolver integration
  - Propagates `store_bom_resolution` from `install` tag to repository rules
  - Handles empty/minimal JSON lock files during parsing
- **`coursier.bzl`** - Coursier resolver integration
  - Adds `store_bom_resolution` attribute to `coursier_fetch` and `pinned_coursier_fetch`
  - Includes `bom_resolution_enabled` in input signature computation
  - Templates `pin.sh` script to conditionally invoke `BomResolverMain`
- **`dependency_tree_parser.bzl`** - BUILD file generation
  - Accepts `bom_resolution` data and emits `maven_bom_coordinate=<bom>` tags

#### Data Structures
- **`V3LockFile`** - v3 lock file format with `bom_resolution` section
- **Lock File Hash Components**:
  - `__INPUT_ARTIFACTS_HASH.bom_resolution_enabled` - Feature state marker
  - `__RESOLVED_ARTIFACTS_HASH` - Unchanged by BOM annotations

### Process Flow
1. **Pin Command**: `REPIN=1 bazel run @repo//:pin`
2. **Dependency Resolution**: Normal artifact resolution (Maven/Coursier)
3. **Lock File Generation**: Basic v3 lock file created
4. **BOM Resolution** (if enabled): `BomResolverMain` post-processes lock file
5. **BUILD Generation**: `dependency_tree_parser.bzl` reads final lock file with BOM data

## Testing

### Unit Tests

#### Starlark Tests (`tests/unit/coursier_test.bzl`)
- **`bom_resolution_flag_changes_input_hash_test`** - Hash differs between enabled/disabled states
- **`bom_resolution_flag_marker_present_when_enabled_test`** - Exact hash value when enabled
- **`bom_resolution_flag_marker_absent_value_when_disabled_test`** - Exact hash value when disabled
- **Anti-faking**: Must call `asserts.false(env, hash_off == hash_on)` - stubs returning same value fail

#### Java Tests (`tests/.../bomresolver/BomResolverTest.java`)
- **`testEmptyInputs`** - Empty BOMs or versionless artifacts return empty mapping
- **`testNoMatchingArtifacts`** - BOMs not managing requested artifacts return empty mapping
- **`testSingleBomManagesArtifact`** - Single BOM managing artifact (uses `org.junit:junit-bom:5.10.0`)
- **`testMultipleBomsSameArtifact`** - Multiple BOMs managing same artifact in declaration order
- **`testRecursiveImportedBoms`** - Parent BOM attribution, not sub-BOM (uses `com.google.cloud:libraries-bom`)
- **`testVersionedArtifactsIgnored`** - Artifacts with explicit versions ignored
- **`testMultipleRepositories`** - Works with multiple Maven repositories
- **Anti-faking**: `assertEquals(expected, actual)` - empty results fail explicitly

### Integration Tests (`tests/bazel_run_tests.sh`)

#### Enabled Cases
- **`test_coursier_resolution_with_boms`** - Coursier resolver with BOM resolution
  - Reset: `echo '{}' > coursier_resolved_install.json`
  - Assert: `jq '.bom_resolution | length' >= 2` and specific mappings exist
- **`test_maven_resolution_with_boms`** - Maven resolver with BOM resolution
  - Reset: `echo '{}' > maven_resolved_install.json`
  - Assert: Selenium BOM mappings for `selenium-api` and `selenium-support`

#### Disabled Cases
- **`test_coursier_resolution_without_bom_resolution`** - Feature disabled
  - Reset: `echo '{}' > coursier_resolved_without_bom_resolution_install.json`
  - Assert: `jq '. | has("bom_resolution") | not'` AND `jq '.__INPUT_ARTIFACTS_HASH | has("bom_resolution_enabled")'`

#### Generated Artifacts
- **`test_jvm_import_bom_tags`** - BUILD file tags validation
  - Assert: `grep -c 'maven_bom_coordinate=' >= 2` and both declared BOMs present

### Smoke Tests

#### End-to-End Validation
- **Pin Commands Work**: `REPIN=1 bazel run @coursier_resolved_with_boms//:pin` succeeds
- **Pin Commands Work**: `REPIN=1 bazel run @maven_resolved_with_boms//:pin` succeeds
- **Build Targets Work**: Generated jvm_import targets build successfully
- **Lock Files Valid**: Generated JSON passes schema validation

### Anti-Faking Test Constraints

#### Mandatory Guardrails (Must Not Be Optional)
1. **Reset Before Assert**: `echo '{}' > <lock-file>` before pin command - prevents stale data false positives
2. **Assert Exact Content**: Use `jq` for exact JSON matching, not `grep -q '"bom_resolution"'` - prevents empty result false positives
3. **Assert Minimum Count**: `len(bom_resolution) >= N` for known N - prevents meaningless empty tests
4. **Negative Case Verification**: When disabled, assert BOTH absence of section AND presence of input hash marker
5. **Hash Comparison Tests**: Assert `hash_enabled != hash_disabled` - prevents "do nothing" implementations
6. **Real Test Execution**: Java tests as `java_test` targets, not `java_binary` - ensures CI actually runs them

#### Break-and-Revert Sanity Check
- Temporarily modify `BomResolver.buildBomResolutionMapping` to return `Collections.emptyMap()`
- Confirm integration tests **fail loudly** with meaningful error messages
- Revert change and confirm tests pass again
- **Critical Rule**: If breaking the resolver doesn't break the tests, the tests aren't testing the resolver

### Test Data Requirements
- **Stable External Dependencies**: Use well-known, stable BOMs like `org.junit:junit-bom:5.10.0`
- **Network Access Acceptable**: Tests require Maven Central access (same as existing integration tests)
- **Deterministic Results**: Assert exact expected mappings, not just "some mapping exists"
- **Multiple Scenarios**: Test single BOM, multiple BOMs, recursive BOMs, and no-BOM cases
