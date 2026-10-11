# Bug 0785: SSTableReader leaked when upgrade already current

**Commit**: 4fff69fcbe · **Ticket**: CASSANDRA-12899
**Subsystem**: Tools / sstableupgrade
**Category**: Resource leak

---

## Root Cause
`StandaloneUpgrader` opens each SSTable via `SSTableReader.openNoValidation()`, which acquires a reference-counted resource handle. When the SSTable's on-disk version already matches the target version the code issued a bare `continue`, skipping the `readers.add()` path without releasing the handle first. Every already-current SSTable therefore leaked its file descriptors and associated memory until the JVM exited.

### Before / After
```java
// BEFORE
SSTableReader sstable = SSTableReader.openNoValidation(entry.getKey(), components, cfs);
if (sstable.descriptor.version.equals(DatabaseDescriptor.getSSTableFormat().info.getLatestVersion()))
    continue;   // <-- reader opened but never released
readers.add(sstable);

// AFTER
SSTableReader sstable = SSTableReader.openNoValidation(entry.getKey(), components, cfs);
if (sstable.descriptor.version.equals(DatabaseDescriptor.getSSTableFormat().info.getLatestVersion()))
{
    sstable.selfRef().release();   // properly release the ref before skipping
    continue;
}
readers.add(sstable);
```

---

## How to Spot
Any code path that calls `SSTableReader.open*()` has an implicit obligation to either add the reader to a container that will close it, or explicitly call `selfRef().release()`. Review every early-exit (`continue`, `return`, exception-catch) in loops that open SSTables and confirm the ref is released before leaving scope. Static analysis rules that flag acquired `Ref<>` objects which escape neither a `try-with-resources` block nor an explicit `release()` call would catch this pattern mechanically.

---

## Test
No test added.
