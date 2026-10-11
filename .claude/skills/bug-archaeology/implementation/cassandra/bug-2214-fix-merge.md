# Bug 2214: CompressedRandomAccessReader close() Silently Swallows IOException

**Commit**: 1b8c475240 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/io/compress/CompressedRandomAccessReader.java
**Category**: Resource leak

---

## Root Cause

After a merge, `CompressedRandomAccessReader.close()` called `super.close()` and then `source.close()`, with the latter declared to throw `IOException`. However, the method signature only declared `throws IOException`, which conflicted with the parent's `close()` signature that did not declare the checked exception. The merge left the method in a state where compile or runtime errors would occur, or the IOException would be silently dropped. The fix wraps `source.close()` in a try/catch that rethrows as `FSReadError`, bringing it in line with Cassandra's convention of translating I/O errors to unchecked runtime exceptions.

### Before / After

```java
// BEFORE
@Override
public void close() throws IOException {
    super.close();
    source.close();
}
```

```java
// AFTER
@Override
public void close() {
    super.close();
    try {
        source.close();
    } catch (IOException e) {
        throw new FSReadError(e, getPath());
    }
}
```

---

## How to Spot

After a merge, inspect any `close()` overrides that changed signature. A `close()` that calls `source.close()` without wrapping the checked exception violates the parent contract and may result in swallowed I/O errors or compilation failures. Cassandra consistently uses `FSReadError`/`FSWriteError` as the unchecked wrappers for I/O exceptions in close paths.

---

## Test

No test added.
