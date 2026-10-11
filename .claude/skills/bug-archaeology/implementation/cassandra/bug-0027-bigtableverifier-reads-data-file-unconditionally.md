# Bug 0027: BigTableVerifier Reads Data File Outside Extended Verification Mode

**Commit**: 817f0b8713 · **Ticket**: CASSANDRA-21150
**Subsystem**: io/sstable/format/big/BigTableVerifier
**Category**: Logic error in condition

---

## Root Cause

A refactor of `BigTableVerifier` introduced unconditional reading of the SSTable data file during the index-block offset check and the trailing `dataFile.reset()` call. The original design only performed this expensive data-file I/O in `extendedVerification` mode. Without the `options.extendedVerification` guard the verifier opened and read data-file blocks even during a normal (non-extended) verification pass, causing `testVerifyCorruptRowCorrectDigest` to fail and imposing unnecessary I/O cost in production.

### Before / After

```java
// BEFORE
if (blockCount > 0)
{
    // reads data file blocks...
}
// ...
dataFile.reset();
```

```java
// AFTER
if (options.extendedVerification && blockCount > 0)
{
    // reads data file blocks...
}
// ...
if (options.extendedVerification)
    dataFile.reset();
```

---

## How to Spot

Whenever a code path is guarded by an option flag (here `extendedVerification`), all sub-operations that depend on that option — including any teardown steps like `reset()` — must be wrapped in the same guard. A common review mistake is to add the guard only to the main branch while leaving cleanup steps unconditional.

---

## Test

The existing `sstable.VerifyTest.testVerifyCorruptRowCorrectDigest` test caught the regression; no new test was added.
