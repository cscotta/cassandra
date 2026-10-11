# Bug 0479: StandardEncryptionManager not serializable due to non-serializable inner class

**Commit**: bfec39f64666b8d49dc5061a6c5cfa96062f613a · **Ticket**: #14751

## Detection Rule

When a class is intended to be serialized (e.g., for Spark distributed tasks) and it holds a non-static inner class instance, verify that the inner class itself is either `Serializable` or marked `transient`, because non-static inner classes hold an implicit reference to the outer instance and break Java serialization.

## Code Smell

A class claimed to be serializable contains a private inner class (`TransientEncryptionState`) that is not `Serializable` and is stored in a `transient` field — but the field is not properly initialized on deserialization, making all post-deserialization method calls throw a `Preconditions` failure rather than a `NotSerializableException` at serialize time.

## Tags

- scope: java
- category: wrong-serialization
- code-pattern: serialize-size-mismatch
- severity: crash
- subsystem: core/encryption/StandardEncryptionManager

## Root Cause

`StandardEncryptionManager` wrapped `KeyManagementClient` and `Map<String, EncryptedKey>` in a non-serializable inner class `TransientEncryptionState` stored as a `transient` field. After deserialization the field was `null`, and every method guarded against this with `Preconditions.checkState(transientState != null, ...)`, causing crashes. The fix made `kmsClient` and `encryptionKeys` direct fields (with `kmsClient` serializable via a lazy-init pattern) and removed the inner class entirely.

## Example

```java
// BEFORE
private class TransientEncryptionState {
    private final KeyManagementClient kmsClient;       // not Serializable
    private final Map<String, EncryptedKey> encryptionKeys;
    ...
}
private final transient TransientEncryptionState transientState;

public ByteBuffer wrapKey(ByteBuffer secretKey) {
    Preconditions.checkState(transientState != null,
        "Cannot wrap key after serialization (missing KMS client)");
    return transientState.kmsClient.wrapKey(secretKey, tableKeyId);
}
```

```java
// AFTER
private final Map<String, EncryptedKey> encryptionKeys; // serializable
private final KeyManagementClient kmsClient;            // now Serializable

public ByteBuffer wrapKey(ByteBuffer secretKey) {
    return kmsClient.wrapKey(secretKey, tableKeyId);
}
```
