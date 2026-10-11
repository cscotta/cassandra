# Bug 0444: Incorrect partition bounds written to manifest when partitioned by binary column after deletion

**Commit**: b7181e8f765fc3ff188c325ba422b347f59d271e · **Ticket**: #15127

## Detection Rule

When a method wraps a raw `byte[]` from a shared or reusable buffer into a `ByteBuffer` and returns it to callers that track min/max statistics, verify that the array is defensively copied before wrapping, because the backing buffer may be mutated or reused during subsequent reads.

## Code Smell

A `ByteBuffer.wrap((byte[]) data[pos])` call that returns a view directly into a mutable object array, where the same array slot is populated from a reader that reuses its backing storage across multiple records.

## Tags

- scope: universal
- category: logic-error
- code-pattern: mutable-shared-buffer
- severity: silent-wrong-result
- subsystem: core/partition-metadata

## Root Cause

`PartitionData.get(int pos)` wraps the stored `byte[]` directly via `ByteBuffer.wrap` without copying. When `ManifestReader` reads multiple files from a manifest, it reuses the same `PartitionData` buffer slot for each entry, so the byte array reference stored in `PartitionFieldStats` (min/max) ends up pointing to the last-read record's bytes rather than the original record's bytes. On deletion, partition bounds in the rewritten manifest silently reflect the wrong partition key.

## Example

```java
// BEFORE
@Override
public Object get(int pos) {
  if (pos >= data.length) {
    return null;
  }
  if (data[pos] instanceof byte[]) {
    return ByteBuffer.wrap((byte[]) data[pos]);  // no copy; shares backing array
  }
  return data[pos];
}
```

```java
// AFTER
@Override
public Object get(int pos) {
  if (pos >= data.length) {
    return null;
  }
  if (data[pos] instanceof byte[]) {
    byte[] copied = Arrays.copyOf((byte[]) data[pos], ((byte[]) data[pos]).length);
    return ByteBuffer.wrap(copied);  // independent copy; safe for stats tracking
  }
  return data[pos];
}
```
