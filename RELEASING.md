The release process is pretty straightforward:

## Pick a version number

The release number uses a `major.minor` version format, and should
just be numbers. If there's a major feature or a breaking change,
don't be shy about bumping the major number: lots of people have a
strong belief in SemVer and it would be a pity to let that faith down.

## Set up a tracking issue in GitHub

This isn't strictly necessary, but it's polite since it allows us to
coordinate the work that we need to complete for the next release.

## Make sure all dependencies are up to date

Check the `bazel_dep` entries and the `maven.install` (named
`rules_jvm_external_deps`) in `MODULE.bazel` and make sure that our
dependencies are the most recent versions. If they're not, create a
PR to update them, and land the change.

## Update the `MODULE.bazel` file to reflect the latest changes

Check that the `version` parameter in the `module` declaration is the
same as the version number of the release you're about to push.

You may well need to create a PR to update this too.

## Tag the release in `git` and push the tag

The tag format is just the release number (eg. `10.5`) without any
additional prefixes or suffixes. We use a `major.minor` version
format.

```shell
git switch master
git pull
git tag XX.YY
git push --tags
```

## Create a draft entry in the GitHub releases.

I like to copy the previous release, and then edit it. There's a handy
button in the UI that will generate some release notes. Use it, then
edit the values it gives you. The release notes should include user
visible changes and all commits by people who aren't core
committers. Bug fixes and little tweaks the core committers have made
can be omitted if you don't think they're particularly noteworthy.

Name the draft release after the version you've picked.

## Create an archive of the release, and add as an asset

GitHub periodically break how archives are created. To avoid this,
package the current release, and upload it as an asset to your draft
release.

This asset must be uploaded before publishing the release because the
BCR publishing workflow downloads it when the release is published.

You can use
`git archive --format=tar --prefix=rules_jvm_external-${TAG}/ ${TAG} | gzip > rules_jvm_external-{TAG}.tar.gz`
to generate the archive.

## Prepare the upload to the BCR

This step is now automated by the `.github/workflows/publish.yaml`
workflow, which delegates to
[`bazel-contrib/publish-to-bcr`](https://github.com/bazel-contrib/publish-to-bcr).

When the GitHub release is published, a workflow run will:

1. Download the release tarball you uploaded in the previous step.
2. Compute its `sha256` integrity hash.
3. Substitute the templated values in `.bcr/*.template.json` into
   `modules/rules_jvm_external/<VERSION>/{MODULE.bazel,source.json,presubmit.yml}`.
4. Append the new version to `modules/rules_jvm_external/metadata.json`.
5. Open a PR against [bazelbuild/bazel-central-registry][bcr] from the
   `bazel-contrib/bazel-central-registry` fork.

A maintainer listed in `metadata.json` should then approve the BCR PR,
and a BCR maintainer will merge it.

If the workflow fails, retry it manually from GitHub Actions >
"Publish to BCR" > "Run workflow", entering the tag name.

If `.bcr/presubmit.yml` needs to change for a release, such as for a
new Bazel version matrix, edit it on `master` before tagging. The
workflow uses whatever is committed at the tag.

## Publish the release

Press the magic buttons in the GitHub UI. This starts the BCR workflow
described above.

[bcr]: https://github.com/bazelbuild/bazel-central-registry
