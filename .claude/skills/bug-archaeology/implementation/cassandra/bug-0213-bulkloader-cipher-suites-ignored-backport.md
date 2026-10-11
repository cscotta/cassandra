# Bug 0213: BulkLoader and Stress Tool Ignore Configured Cipher Suites for TLS (4.0 Backport)

**Commit**: 4a2a585ec0 · **Ticket**: CASSANDRA-18582
**Subsystem**: src/java/org/apache/cassandra/tools/BulkLoader.java · src/java/org/apache/cassandra/config/EncryptionOptions.java · tools/stress/src/org/apache/cassandra/stress/util/JavaDriverClient.java
**Category**: Wrong constant / default value

---

## Root Cause

This is the 4.0 branch backport of the same defect documented in bug-0212.
`BulkLoader` and `JavaDriverClient` passed `null` for cipher suites when
constructing `RemoteEndpointAwareJdkSSLOptions`, silently discarding any
`--ssl-ciphers` configuration.  In addition, `EncryptionOptions.cipherSuitesArray()`
returned `new String[0]` (meaning "no suites") instead of `null` (meaning "use
defaults") when no cipher suites were configured, which would have the wrong effect
if the empty-array path were ever reached.  Both issues were present in the 4.0
code base and required a separate fix commit targeting that branch.

### Before / After

```java
// BEFORE
RemoteEndpointAwareJdkSSLOptions sslOptions =
    new RemoteEndpointAwareJdkSSLOptions(sslContext, null) { ... }

return cipher_suites == null ? new String[0] : cipher_suites.toArray(new String[0]);
```

```java
// AFTER
RemoteEndpointAwareJdkSSLOptions sslOptions =
    new RemoteEndpointAwareJdkSSLOptions(sslContext, clientEncryptionOptions.cipherSuitesArray()) { ... }

return cipher_suites == null ? null : cipher_suites.toArray(new String[0]);
```

---

## How to Spot

Same heuristic as the trunk version: audit constructor calls that accept
security-sensitive parameters for literal `null` arguments, and be alert to
"empty array vs. null" distinctions in SSL/TLS library APIs.  Backport commits
that adjust a helper method's return value for the unconfigured case indicate
the bug appeared in multiple branches simultaneously.

---

## Test

`SSTableLoaderEncryptionOptionsTest` extended to pass `--ssl-ciphers` and assert a
clean exit with correct output.
