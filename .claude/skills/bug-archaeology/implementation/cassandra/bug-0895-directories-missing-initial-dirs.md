# Bug 0895: Directories Created Without initialDirectories, Missing Configured Data Paths

**Commit**: 7eb464734b · **Ticket**: CASSANDRA-11849
**Subsystem**: Storage / Data Directories
**Category**: Wrong constant / default value

---

## Root Cause
Three call sites — `ColumnFamilyStore.scrubDataDirectories()`, `LogTransaction.removeUnfinishedLeftovers(CFMetaData)`, and `StandaloneSSTableUtil.listFiles()` — constructed a `Directories` object using only `new Directories(metadata)`, which falls back to the default directory discovery logic rather than using the pre-computed `CFS.initialDirectories` list. This meant those code paths could scan a different (potentially incomplete) set of data directories than the live CFS, causing scrub or cleanup operations to silently miss SSTables in non-default directories, and leaving orphaned transaction log files unresolved. The fix passes `initialDirectories` (or `ColumnFamilyStore.getInitialDirectories()` for static contexts) as the second argument to `Directories` in all three sites.

### Before / After
```java
// BEFORE — uses default directory discovery, may miss configured dirs
Directories directories = new Directories(metadata);

// AFTER — uses the same directory list as the live CFS
Directories directories = new Directories(metadata, initialDirectories);

// And in static contexts:
new Directories(metadata, ColumnFamilyStore.getInitialDirectories())
```

---

## How to Spot
When constructing `Directories` outside of an already-initialized `ColumnFamilyStore`, look for whether the two-argument constructor form is used. The single-argument constructor is a footgun in any path that must be consistent with the live CFS — its use is a signal that the code was written when only one data directory was expected or that the second parameter was overlooked. Review every maintenance path (scrub, cleanup, tools) to ensure they share the same directory enumeration as the main read/write path.

---

## Test
No test added.
