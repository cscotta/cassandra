# Bug 0212: BulkLoader and Stress Tool Ignore Configured Cipher Suites for TLS

**Commit**: 1c7e7db2d0 · **Ticket**: CASSANDRA-18582
**Subsystem**: src/java/org/apache/cassandra/tools/BulkLoader.java · src/java/org/apache/cassandra/config/EncryptionOptions.java · tools/stress/src/org/apache/cassandra/stress/util/JavaDriverClient.java
**Category**: Wrong constant / default value

---

## Root Cause

`BulkLoader` and the stress tool's `JavaDriverClient` constructed
`RemoteEndpointAwareJdkSSLOptions` by passing a hard-coded `null` for the cipher
suites parameter, regardless of what was configured via `--ssl-ciphers` or the
`EncryptionOptions` configuration.  The `cipherSuitesArray()` helper on
`EncryptionOptions` additionally returned an empty array (`new String[0]`) when
`cipher_suites` was `null`; since the SSL library interprets an empty array as
"no suites allowed" differently from `null` ("use all defaults"), that helper
was also corrected to return `null` when unconfigured.  The combined effect meant
any explicitly requested cipher suite restriction was silently discarded.

### Before / After

```java
// BEFORE – cipher suites ignored; helper returned empty array instead of null
RemoteEndpointAwareJdkSSLOptions sslOptions =
    new RemoteEndpointAwareJdkSSLOptions(sslContext, null) { ... }

// EncryptionOptions.cipherSuitesArray()
return cipher_suites == null ? new String[0] : cipher_suites.toArray(new String[0]);
```

```java
// AFTER – configured suites passed; helper returns null when unconfigured
RemoteEndpointAwareJdkSSLOptions sslOptions =
    new RemoteEndpointAwareJdkSSLOptions(sslContext, clientEncryptionOptions.cipherSuitesArray()) { ... }

// EncryptionOptions.cipherSuitesArray()
return cipher_suites == null ? null : cipher_suites.toArray(new String[0]);
```

---

## How to Spot

When a configuration object is passed to a constructor or builder, verify that
every configured field is actually threaded through; passing a literal `null`
where a user-facing config option should flow is a common silent-ignore pattern.
Also watch for "empty array vs. null" distinctions in library APIs where the two
have materially different semantics.

---

## Test

`SSTableLoaderEncryptionOptionsTest` extended to pass `--ssl-ciphers` and assert a
clean exit with correct output.
