# Bug 0330: Snapshot summary omits manifest created/replaced/kept counts

**Commit**: c31dd921e1550c643a3ea1fce9d9573cc06e2607 · **Ticket**: #15003

## Detection Rule

When a snapshot is committed and a summary map is built, verify that all documented summary keys (including manifest lifecycle metrics such as created, replaced, and kept counts) are populated for every commit path, not just some.

## Code Smell

A `SnapshotSummary.Builder` is assembled inside `prepareNewSnapshot()` / `apply()` paths without any calls to set `manifests-created`, `manifests-replaced`, or `manifests-kept` keys, while other summary keys (e.g., `added-data-files`) are set correctly — indicating a missing instrumentation branch.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: metric-not-populated-on-path
- severity: silent-wrong-result
- subsystem: core/snapshot-producer

## Root Cause

The snapshot summary builder was never wired up to count how many manifests were newly created, kept from the previous snapshot, or replaced (rewritten) during a commit. The counts were computable from the committed manifest list (snapshotId comparison) and from the filter/merge managers that already tracked replacements internally, but no code path ever called `summaryBuilder.set(CREATED_MANIFESTS_COUNT, ...)` and friends. All three metrics were simply absent from every committed snapshot's summary map.

## Example

```java
// BEFORE — FastAppend.prepareNewSnapshot(), no manifest count built
protected List<ManifestFile> apply(...) {
    // ...
    manifests.addAll(snapshot.allManifests(ops().io()));
    return manifests;  // summary has no manifest count entries
}
```

```java
// AFTER — manifest count summary merged before returning
protected List<ManifestFile> apply(...) {
    // ...
    manifests.addAll(snapshot.allManifests(ops().io()));
    summaryBuilder.merge(buildManifestCountSummary(manifests, 0));
    return manifests;
}

// helper (in SnapshotProducer)
protected SnapshotSummary.Builder buildManifestCountSummary(
    List<ManifestFile> manifests, int replacedManifestsCount) {
    int created = 0, kept = 0;
    for (ManifestFile m : manifests) {
        if (snapshotId() == m.snapshotId()) created++;
        else if (m.snapshotId() != null) kept++;
    }
    builder.set(CREATED_MANIFESTS_COUNT, String.valueOf(created));
    builder.set(KEPT_MANIFESTS_COUNT,    String.valueOf(kept));
    builder.set(REPLACED_MANIFESTS_COUNT, String.valueOf(replacedManifestsCount));
    return builder;
}
```
