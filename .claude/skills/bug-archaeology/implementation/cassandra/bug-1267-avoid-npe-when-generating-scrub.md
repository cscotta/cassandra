# Bug 1267: Avoid NPE When Generating Scrub Error Message

**Commit**: dbb36a476b · **Ticket**: CASSANDRA-9290
**Subsystem**: src/java/org/apache/cassandra/db/compaction/Scrubber.java
**Category**: Missing null / bounds check

---

## Root Cause

In `Scrubber`, when validating data-file row keys against the index file, the code compared `key.key` against `currentIndexKey` unconditionally. When `currentIndexKey` is null (i.e., the index file is absent or its first key has not yet been read), calling `key.key.equals(currentIndexKey)` does not throw — but passing a null value into `ByteBufferUtil.bytesToHex(currentIndexKey)` inside the exception-message format string does, producing an NPE in the error-handling path instead of the informative `IOError`. The fix guards the comparison so the branch is only entered when `currentIndexKey` is non-null.

### Before / After

```java
// BEFORE
if (!key.key.equals(currentIndexKey))
{
    throw new IOError(new IOException(String.format(
        "Key from data file (%s) does not match key from index file (%s)",
        ByteBufferUtil.bytesToHex(key.key), ByteBufferUtil.bytesToHex(currentIndexKey))));
```

```java
// AFTER
if (currentIndexKey != null && !key.key.equals(currentIndexKey))
{
    throw new IOError(new IOException(String.format(
        "Key from data file (%s) does not match key from index file (%s)",
        ByteBufferUtil.bytesToHex(key.key), ByteBufferUtil.bytesToHex(currentIndexKey))));
```

---

## How to Spot

Any time a variable used inside an exception message or error-formatting call can be null, the guard condition for entering that block must also protect against null. Code-review for NPE-in-error-path bugs by checking that every variable referenced inside a `String.format` or similar call within a `throw` statement is guaranteed non-null at that point.

---

## Test

No test added.
