# Bug 0482: Flink dynamic sink merges write results across partition specs into a single manifest

**Commit**: dc708e675d263bd09f95f11421a3fd77e430aca6 · **Ticket**: n/a

## Detection Rule

When write results from multiple partition specs are aggregated before writing manifests, verify that results are grouped by spec ID before creating each manifest, not merged together under a single combined manifest file.

## Code Smell

An aggregation map keyed by `WriteTarget` (which includes `specId`) collects results, but then passes all results for a given `(tableName, branch)` to a single `writeToManifest()` call that uses only one partition spec — the spec from the first `WriteTarget` key — ignoring that different results may belong to different specs.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: grouping-key-not-propagated
- severity: silent-wrong-result
- subsystem: flink/dynamic-sink/aggregator

## Root Cause

`DynamicWriteResultAggregator` grouped results by `WriteTarget` (which encodes `tableName`, `branch`, AND `specId`), but then wrote all results to a single manifest using whatever spec came from a single key. When a checkpoint contained data files for different partition specs (e.g. during partition evolution), write results for spec B could be serialized into a manifest produced with spec A's layout, producing a corrupt or mismatched manifest. The fix groups results by `(tableName, branch)` first and then by `specId`, writing a separate per-spec manifest for each group.

## Example

```java
// BEFORE: all results for a (table, branch) written to one manifest regardless of spec
byte[] writeToManifest(
    WriteTarget key, Collection<DynamicWriteResult> writeResults, long checkpointId) {
  if (writeResults.isEmpty()) {
    return EMPTY_MANIFEST_DATA;
  }
  WriteResult.Builder builder = WriteResult.builder();
  writeResults.forEach(w -> builder.add(w.writeResult()));
  // uses key.specId() — but writeResults may contain mixed specs
  DeltaManifests deltaManifests =
      FlinkManifestUtil.writeCompletedFiles(result, factory, spec(key.tableName(), key.specId()), version);
  return SimpleVersionedSerialization.writeVersionAndSerialize(INSTANCE, deltaManifests);
}
```

```java
// AFTER: results grouped by specId, one manifest per spec
byte[][] writeToManifests(
    String tableName, Map<Integer, Collection<WriteResult>> writeResultsBySpec, long checkpointId) {
  byte[][] deltaManifestsBySpec = new byte[writeResultsBySpec.size()][];
  int idx = 0;
  for (Map.Entry<Integer, Collection<WriteResult>> entry : writeResultsBySpec.entrySet()) {
    deltaManifestsBySpec[idx] =
        writeToManifest(tableName, entry.getKey(), entry.getValue(), checkpointId);
    idx++;
  }
  return deltaManifestsBySpec;
}
```
