# Bug 1800: Fix User Defined Compaction

**Commit**: be21417570 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Wrong constant / default value

---

## Root Cause

`forceUserDefinedCompaction` constructed a `File` from a relative path built by concatenating the keyspace and column-family names, then called `Descriptor.fromFilename(directory, filename)` with that relative directory as context. This caused the descriptor lookup to resolve to the wrong absolute path, making `lookupSSTable` fall back to a fragile `endsWith` string comparison rather than an exact `equals` check, and breaking the whole flow whenever the data directory was not directly relative to the working directory. The fix adds a `Directories.find(filename)` method that searches the actual configured data directories, and groups compaction work by `ColumnFamilyStore` directly instead of by a `Pair<String,String>` key.

### Before / After

```java
// BEFORE
File directory = new File(desc.ksname + File.separator + desc.cfname);
Pair<Descriptor, String> p = Descriptor.fromFilename(directory, filename.trim());
Pair<String, String> key = Pair.create(p.left.ksname, p.left.cfname);
descriptors.put(key, p.left);
// ...lookup uses endsWith:
if (sstable.descriptor.toString().endsWith(descriptor.toString()))
```

```java
// AFTER
ColumnFamilyStore cfs = Keyspace.open(desc.ksname).getColumnFamilyStore(desc.cfname);
descriptors.put(cfs, cfs.directories.find(filename.trim()));
// ...lookup uses exact equals:
if (sstable.descriptor.equals(descriptor))
```

---

## How to Spot

Whenever a path is constructed by concatenating logical names (keyspace/CF) instead of using the configured data directory list, the resulting path is environment-dependent. Look for any `new File(ksname + separator + cfname)` patterns and verify they are resolved against real data directories.

---

## Test

No test added.
