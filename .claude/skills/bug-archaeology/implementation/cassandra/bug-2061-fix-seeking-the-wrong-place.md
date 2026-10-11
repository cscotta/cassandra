# Bug 2061: Compressed Stream Seek Skipped When NIO Unavailable

**Commit**: 128177c412 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/streaming/compress/CompressedFileStreamTask.java
**Category**: Off-by-one

---

## Root Cause

`CompressedFileStreamTask` iterates over multiple file sections and streams each one. The `seek(section.left)` call was placed inside the `else` branch that handles the non-NIO (inter-node encryption) fallback path, but it was reached only on the second and subsequent loop iterations — the first section started at the correct position by coincidence. The seek was missing before the very first section in the non-NIO path, and for subsequent sections in that path, the seek happened after the transfer buffer was already set up, interleaving seeks incorrectly with buffer allocation. The fix moves the seek to the top of the loop body, conditioned on the NIO socket channel being unavailable (`sc == null`), so it always executes before any transfer logic.

### Before / After

```java
// BEFORE (seek was inside the else/NIO-fallback branch, too late)
for (Pair<Long, Long> section : sections) {
    long length = section.right - section.left;
    // ... NIO path ...
    } else {
        file.seek(section.left); // wrong position: only reached mid-loop
        // transfer buffer allocation and write
    }
}
```

```java
// AFTER
for (Pair<Long, Long> section : sections) {
    if (sc == null)
        file.seek(section.left); // seek before any section logic
    long length = section.right - section.left;
    // ...
}
```

---

## How to Spot

When streaming non-contiguous file sections, confirm that a seek to `section.start` is always executed before any read of that section, for every code path (NIO and non-NIO). Missing a seek in any branch means the second section will begin reading from wherever the previous section left off.

---

## Test

No test added.
