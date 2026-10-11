# Bug 0537: ManifestFileBean.keyMetadata() always returns null, silently dropping encryption metadata

**Commit**: aee89008aaebb99eb26ddbd3f79220a30a7ef6c9 · **Ticket**: #14750

## Detection Rule

When a data-transfer bean implements an interface method by hard-coding a return value (e.g., `return null`) instead of delegating to a field, verify that the field is either intentionally excluded from the interface contract or is actually populated and returned.

## Code Smell

An `@Override` method in a serializable bean that returns a compile-time constant (`return null`) while a corresponding field for that data exists (or should exist) in the class.

## Tags

- scope: java
- category: missing-override
- code-pattern: null-after-map-lookup
- severity: silent-wrong-result
- subsystem: spark/actions

## Root Cause

`ManifestFileBean` is a Spark-serializable bean that implements the `ManifestFile` interface. The `keyMetadata()` method was stubbed to always return `null` and no backing field was present, so when Spark actions read manifests from the `all_manifests` metadata table and reconstructed them as `ManifestFileBean` objects, encryption key metadata was silently discarded. Downstream consumers (e.g., rewrite jobs) then operated on manifests without key information, which would fail or silently skip decryption.

## Example

```java
// BEFORE
public class ManifestFileBean implements ManifestFile, Serializable {
    // no keyMetadata field

    @Override
    public ByteBuffer keyMetadata() {
        return null; // always null — field never stored
    }
}
```

```java
// AFTER
public class ManifestFileBean implements ManifestFile, Serializable {
    private byte[] keyMetadata = null;

    public static ManifestFileBean fromManifest(ManifestFile manifest) {
        bean.setKeyMetadata(
            manifest.keyMetadata() == null ? null : manifest.keyMetadata().array());
        return bean;
    }

    @Override
    public ByteBuffer keyMetadata() {
        return keyMetadata == null ? null : ByteBuffer.wrap(keyMetadata);
    }
}
```
