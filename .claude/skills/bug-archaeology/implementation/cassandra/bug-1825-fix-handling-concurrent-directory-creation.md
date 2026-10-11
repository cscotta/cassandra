# Bug 1825: FSWriteError on Concurrent Directory Creation

**Commit**: 510eabfeb4 · **Ticket**: CASSANDRA-6459
**Subsystem**: src/java/org/apache/cassandra/db
**Category**: Race condition / missing synchronization

---

## Root Cause

`Directories.createDirectory()` called `dir.mkdirs()` and treated a `false` return as an unrecoverable failure, immediately throwing `FSWriteError`. However, `mkdirs()` returns `false` both when creation actually fails and when another concurrent thread created the same directory between the `!dir.exists()` check and the `mkdirs()` call. The fix adds a secondary check: if `mkdirs()` returns `false` but the directory now exists and is a valid directory, the concurrent-creation case is treated as success.

### Before / After

```java
// BEFORE
else if (!dir.mkdirs())
{
    throw new FSWriteError(new IOException("Unable to create directory " + dir), dir);
}
```

```java
// AFTER
else if (!dir.mkdirs() && !(dir.exists() && dir.isDirectory()))
{
    throw new FSWriteError(new IOException("Unable to create directory " + dir), dir);
}
```

---

## How to Spot

Whenever `mkdirs()` / `mkdir()` is used and the boolean result drives an error path, verify whether a concurrent creator could race between the existence check and the creation call. The TOCTOU pattern (`exists()` → `mkdirs()`) is the classic signal.

---

## Test

A multithreaded test (`testMTSnapshots`) was added that invokes `getSnapshotDirectory` from two threads simultaneously on the same snapshot tag and asserts both returned directories exist without error.
