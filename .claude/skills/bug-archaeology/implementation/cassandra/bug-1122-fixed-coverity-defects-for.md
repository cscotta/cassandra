# Bug 1122: Charset-Dependent Checksum Corruption in TransactionLog

**Commit**: 00bcccc8c9 · **Ticket**: CASSANDRA-7066
**Subsystem**: db/lifecycle/TransactionLog, io/util/FileUtils
**Category**: Wrong constant / default value

---

## Root Cause

`String.getBytes()` with no argument uses the JVM's default platform charset, which varies by OS and locale. In `TransactionLog`, two call sites used the bare `getBytes()` form when computing and verifying a CRC checksum over log record content. If the platform charset differed between the node that wrote the log and the node (or JVM restart) that read it, the bytes fed to the checksum would differ, causing records to appear corrupt. The fix introduces a named `FileUtils.CHARSET` constant (`StandardCharsets.UTF_8`) and uses it consistently for all `getBytes()` calls and file read/write operations.

### Before / After

```java
// BEFORE
return record.getBytes();
// ...
byte[] bytes = matcher.group(1).getBytes();
```

```java
// AFTER
return record.getBytes(FileUtils.CHARSET);
// ...
byte[] bytes = matcher.group(1).getBytes(FileUtils.CHARSET);
```

---

## How to Spot

Any call to `String.getBytes()` or `new String(bytes)` without an explicit charset argument is a latent portability bug. In checksum or signature verification code this becomes a correctness hazard: the same byte sequence produces different hashes on machines with different default charsets. Reviewers should flag all bare `getBytes()` uses and insist on `StandardCharsets.UTF_8` (or another explicit constant) at every call site.

---

## Test

No test added.
