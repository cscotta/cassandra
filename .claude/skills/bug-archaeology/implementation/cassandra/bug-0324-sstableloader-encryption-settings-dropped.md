# Bug 0324: Streaming Encryption Settings Silently Dropped in BulkLoadConnectionFactory

**Commit**: a8bbd22d37 · **Ticket**: CASSANDRA-17367
**Subsystem**: tools/BulkLoadConnectionFactory
**Category**: Wrong constant / default value

---

## Root Cause

`BulkLoadConnectionFactory.create` correctly switched to the secure storage port when `internode_encryption` was not `none`, but it never called `.withEncryption(encryptionOptions)` on the `OutboundConnectionSettings` template. The connection was therefore directed at the TLS port while the underlying `OutboundConnectionSettings` still held the default (no-encryption) settings, causing the TLS handshake to fail or fall back to plain text. The encryption options passed to the constructor were effectively silently ignored once the factory was invoked.

The previous test only verified that the loader could connect on the native port; it did not assert that any rows were actually streamed, so the missing `.withEncryption()` call had gone undetected.

### Before / After

```java
// BEFORE
template = template.withConnectTo(template.to.withPort(secureStoragePort));
```

```java
// AFTER
template = template.withConnectTo(template.to.withPort(secureStoragePort))
                   .withEncryption(encryptionOptions);
```

---

## How to Spot

When a class receives configuration options via constructor injection and later builds a connection template in a helper method, review every builder chain to confirm that all injected options are actually applied. It is easy to set the port (a scalar that causes an obvious failure if wrong) while silently omitting a more complex option object. Integration tests for encrypted paths should assert data was actually transferred end-to-end, not just that no exception was thrown.

---

## Test

Added `bulkLoaderSuccessfullyStreamsOverSsl`: inserts 42 rows, flushes to SSTable, truncates, bulk-loads over TLS, and asserts `SELECT count(*) = 42`.
