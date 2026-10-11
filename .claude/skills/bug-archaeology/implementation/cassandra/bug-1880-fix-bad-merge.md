# Bug 1880: Bad Merge Left Conflict Markers in LeveledCompactionStrategy

**Commit**: f19610c1a9 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/LeveledCompactionStrategy.java
**Category**: Logic error in condition

---

## Root Cause

A three-way merge was not cleanly resolved, leaving raw Git conflict markers (`<<<<<<< HEAD`, `|||||||`, `=======`, `>>>>>>>`) inside the constructor of `LeveledCompactionStrategy`. The surviving code was a garbled mixture of two branches' versions of the `sstable_size_option` validation block. The fix replaced it with the correct post-merge logic: a `null`-guarded options check that reads the size option, conditionally warns if it is too large (>= 1000 MB), and also warns if it is unusually small (< 50 MB), using the correct `cfs.name` field name from the newer branch.

### Before / After

```java
// BEFORE — merge conflict markers left in source
if (options.containsKey(SSTABLE_SIZE_OPTION))
{
    configuredMaxSSTableSize = Integer.parseInt(options.get(SSTABLE_SIZE_OPTION));
    if (configuredMaxSSTableSize >= 1000)
    {
<<<<<<< HEAD
        logger.warn("Max sstable size of {}MB ...", configuredMaxSSTableSize);
||||||| ...
        // duplicate/garbled block
=======
        if (!Boolean.getBoolean("cassandra.tolerate_sstable_size"))
        { ... }
>>>>>>> cassandra-1.2
    }
}
```

```java
// AFTER — resolved merge with null-guard and correct field name
if (options != null)
{
    if (options.containsKey(SSTABLE_SIZE_OPTION))
    {
        configuredMaxSSTableSize = Integer.parseInt(options.get(SSTABLE_SIZE_OPTION));
        if (!Boolean.getBoolean("cassandra.tolerate_sstable_size"))
        {
            if (configuredMaxSSTableSize >= 1000)
                logger.warn("... {}MB ...", configuredMaxSSTableSize, cfs.name, cfs.getColumnFamilyName());
            if (configuredMaxSSTableSize < 50)
                logger.warn("... {}MB ...", configuredMaxSSTableSize, cfs.name, cfs.getColumnFamilyName());
        }
    }
}
```

---

## How to Spot

Always compile the codebase after resolving merges; conflict markers cause a compile error, so CI will catch this immediately. Also, search for the literal strings `<<<<<<<`, `=======`, and `>>>>>>>` in source files as part of pre-commit hooks or CI lint steps.

---

## Test

No test added. The defect is a compile-time failure rather than a runtime behavior regression.
