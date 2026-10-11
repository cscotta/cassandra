# Bug 0038: Manifest inheritance silently accepts differing data and file sequence numbers

**Commit**: 8f611675e343c845c9df6e56718f1492aeb143e0 · **Ticket**: #16091

## Detection Rule

When an inheritance or copy operation applies a value from a source object that conflates two logically distinct fields into one, verify that the source object asserts those fields are equal before using either value, so callers cannot silently propagate a corrupt or mismatched state.

## Code Smell

An `inheritFrom` method that reads `source.fileSequenceNumber()` and applies it to two separate destination fields (`dataSequenceNumber` and `fileSequenceNumber`) without first asserting `source.dataSequenceNumber() == source.fileSequenceNumber()`, paired with a comment noting that manifests "do not distinguish" between the two.

## Tags

- scope: project-specific
- category: logic-error
- code-pattern: missing-invariant-assertion
- severity: silent-wrong-result
- subsystem: core/manifest

## Root Cause

Manifests store a single sequence number that serves as both the data sequence number and the file sequence number for entries. The `inheritFrom` path in `TrackingStruct` applied the manifest's `fileSequenceNumber` to both fields but never validated that `dataSequenceNumber == fileSequenceNumber` on the manifest itself. A caller supplying a manifest `Tracking` with mismatched values would silently produce a `TrackingStruct` with inconsistent sequence numbers. Adding a `Preconditions.checkArgument` equality guard catches this at the point of inheritance rather than allowing corrupt metadata to propagate.

## Example

```java
// BEFORE (no validation; comment acknowledges the conflation)
// both sequence numbers inherit from file sequence number because manifests
// do not distinguish between data and file sequence numbers
if (status == EntryStatus.ADDED) {
  if (dataSequenceNumber == null) {
    this.dataSequenceNumber = manifestTracking.fileSequenceNumber();
  }
  ...
}
```

```java
// AFTER
Preconditions.checkArgument(
    Objects.equals(
        manifestTracking.dataSequenceNumber(), manifestTracking.fileSequenceNumber()),
    "Manifest data and file sequence numbers must be equal, got %s and %s",
    manifestTracking.dataSequenceNumber(),
    manifestTracking.fileSequenceNumber());
if (status == EntryStatus.ADDED) {
  if (dataSequenceNumber == null) {
    this.dataSequenceNumber = manifestTracking.fileSequenceNumber();
  }
  ...
}
```
