# Bug 1188: Digest Mismatch Assertion Prints Raw Object Reference Instead of Hex Content

**Commit**: 9ff5d44c42 · **Ticket**: CASSANDRA-9460
**Subsystem**: src/java/org/apache/cassandra/db/ReadResponse.java
**Category**: Wrong constant / default value

---

## Root Cause

The assertion error message for a digest mismatch called `digest.array()` directly inside `String.format`, which formats a `byte[]` as its default Java object reference (e.g., `[B@1a2b3c`) rather than the actual byte values. When a digest conflict was detected, the logged message was unreadable and provided no useful diagnostic information about what the two conflicting digests actually contained.

### Before / After

```java
// BEFORE
assert digest.equals(this.digest) :
    String.format("Digest mismatch : %s vs %s", digest.array(), this.digest.array());
```

```java
// AFTER
assert digest.equals(this.digest) :
    String.format("Digest mismatch : %s vs %s",
                  Arrays.toString(digest.array()),
                  Arrays.toString(this.digest.array()));
```

---

## How to Spot

Passing a `byte[]` directly to `String.format` with `%s` always produces a useless object reference. Whenever a diagnostic message (assertion, exception, or log) needs to display binary data, the array must be wrapped with `Arrays.toString()`, `ByteBufferUtil.bytesToHex()`, or a similar utility. Code review should flag any `%s` format argument whose static type is an array type.

---

## Test

No test added.
