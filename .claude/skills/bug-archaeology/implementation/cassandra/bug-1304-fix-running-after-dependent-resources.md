# Bug 1304: SSTableReader.runOnClose Executes After Dependent Resources Freed

**Commit**: c397165dcf · **Ticket**: CASSANDRA-9021
**Subsystem**: io/sstable/SSTableReader, io/util/SafeMemory
**Category**: Resource leak

---

## Root Cause

When an SSTableReader is closed, a sequence of dependent resources must be released in the correct order: `dfile` and `ifile` must remain valid until the `runOnClose` callback has finished, because the callback may access those files. The bug, introduced by CASSANDRA-8746, placed the `dfile.close()` and `ifile.close()` calls *before* the `runOnClose.run()` invocation, so the callback could operate on already-freed memory. A separate but related problem in `SafeMemory` left `peer` pointing to freed native memory after `free()` or `close()`, which means any subsequent read through that pointer would silently corrupt data or crash.

### Before / After

```java
// BEFORE – files closed before runOnClose fires
bf.close();
dfile.close();
ifile.close();
if (summary != null)
    summary.close();
if (runOnClose != null)
    runOnClose.run();
typeRef.release();
```

```java
// AFTER – runOnClose fires while dfile/ifile are still open
bf.close();
if (summary != null)
    summary.close();
if (runOnClose != null)
    runOnClose.run();
dfile.close();
ifile.close();
typeRef.release();
```

Also in `SafeMemory`:

```java
// BEFORE
public void free() { ref.release(); }
public void close() { ref.ensureReleased(); }
```

```java
// AFTER – nulls the native pointer immediately
public void free()  { ref.release();        peer = 0; }
public void close() { ref.ensureReleased(); peer = 0; }
```

---

## How to Spot

Whenever a resource-close sequence contains a user-supplied callback (`runOnClose`, `onClose`, `afterClose`), check that the callback fires *before* the resources it is permitted to touch are released. The correct pattern is to close resources in dependency order: close what the callback needs last, not first. Additionally, any native-memory wrapper that releases a pointer should zero the pointer field immediately to prevent use-after-free access through a dangling reference.

---

## Test

No test added.
