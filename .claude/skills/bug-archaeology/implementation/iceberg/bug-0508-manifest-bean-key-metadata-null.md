# Bug 0508: ManifestFileBean keyMetadata always null in manifest tables

**Commit**: d5b83fc3a5035244ba279a1204e52efe118e7f46 · **Ticket**: #15041

## Detection Rule

When a bean class wraps an interface that has a `ByteBuffer`-returning method, and the bean stores the value as a primitive `byte[]` for serialization, verify that the getter reconstructs the `ByteBuffer` from the stored array rather than returning a hardcoded `null`.

## Code Smell

A `@Override` method in a bean/DTO class that returns a field type (e.g., `ByteBuffer`) but has a `return null` body, while the class also declares a private field that could hold the data.

## Tags

- scope: java
- category: wrong-constant
- code-pattern: hardcoded-null-return
- severity: silent-wrong-result
- subsystem: spark-actions manifest-table

## Root Cause

`ManifestFileBean` serializes manifest metadata to Spark datasets. Its `keyMetadata` field was added as `byte[]` for Spark compatibility, with proper getter/setter. However, the `keyMetadata()` method required by the `ManifestFile` interface was left as `return null` — it was never wired to the stored `byte[]` field. As a result, encryption key metadata was silently dropped whenever manifest tables were built via Spark actions.

## Example

```java
// BEFORE
private byte[] keyMetadata = null;  // field exists but is never used

@Override
public ByteBuffer keyMetadata() {
    return null;  // always null regardless of stored field
}
```

```java
// AFTER
private byte[] keyMetadata = null;

@Override
public ByteBuffer keyMetadata() {
    return keyMetadata == null ? null : ByteBuffer.wrap(keyMetadata);
}
```
