# Bug 0190: Avro compression settings not propagated to manifest writers

**Commit**: 08ac7844d2e223c3c4209f2f941acf4dd55c1bfb · **Ticket**: #15652

## Detection Rule

When a subsystem that writes Avro files exposes table-level compression properties, verify that those properties are read and forwarded to every `ManifestWriter` construction site, not only to data-file writers.

## Code Smell

`ManifestFiles.write(...)` and `ManifestFiles.writeDeleteManifest(...)` call sites in `SnapshotProducer` that pass no writer-properties argument, while nearby `TableProperties.AVRO_COMPRESSION` constants exist and are used for data writers but not for manifest writers.

## Tags

- scope: project-specific
- category: silent-skip
- code-pattern: config-not-forwarded
- severity: silent-wrong-result
- subsystem: core/manifest

## Root Cause

`SnapshotProducer` reads table-level properties for data-file Avro compression, but never read or forwarded the analogous manifest compression properties (`write.manifest.compression-codec`, `write.manifest.compression-level`) to the internal `ManifestWriter` instances. As a result, manifest files were always written with the Avro default codec regardless of table configuration, wasting storage or ignoring explicit user tuning.

## Example

```java
// BEFORE — no writer properties passed; compression setting ignored
protected ManifestWriter<DataFile> newManifestWriter(PartitionSpec spec) {
    return ManifestFiles.write(
        ops.current().formatVersion(), spec, newManifestOutputFile(), snapshotId());
}
```

```java
// AFTER — table's manifest compression properties forwarded
protected ManifestWriter<DataFile> newManifestWriter(PartitionSpec spec) {
    return ManifestFiles.write(
        ops.current().formatVersion(),
        spec,
        newManifestOutputFile(),
        snapshotId(),
        manifestWriterProps);  // built from MANIFEST_COMPRESSION table properties
}
```
