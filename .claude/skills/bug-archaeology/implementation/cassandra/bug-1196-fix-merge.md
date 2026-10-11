# Bug 1196: Scrub Passes Hardcoded False for Offline Flag, NPE When Index File Missing

**Commit**: 0f5dd225de · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/CompactionManager.java
**Category**: Wrong constant / default value

---

## Root Cause

A prior merge introduced an `offline` parameter to `scrubOne()` and the `Scrubber` constructor, intended to allow the standalone scrubber tool to run without live index files. However, `performScrub()` in `CompactionManager` called `scrubOne(cfs, input, skipCorrupted, false, checkData)` with a hardcoded `false` for the `offline` flag, ignoring the value passed by `StandaloneScrubber`. This meant offline scrubs always initialised the scrubber in online mode, which attempted to open the index file (`ifile`) and later dereferenced it without a null check, causing an NPE when the index file was absent. A separate null guard was also added in `BigTableReader` for `ifile`.

### Before / After

```java
// BEFORE — offline flag hardcoded to false, ignores caller intent
try (Scrubber scrubber = new Scrubber(cfs, modifier, skipCorrupted, false, checkData))
```

```java
// AFTER — offline flag threaded through from performScrub parameter
try (Scrubber scrubber = new Scrubber(cfs, modifier, skipCorrupted, offline, checkData))
```

---

## How to Spot

When a method signature is extended with a new boolean parameter, grep for every call site and confirm none still pass a literal constant where the new parameter should be forwarded. Hardcoded `false` or `true` at a call site that previously matched the only value are classic signs the parameter was not fully plumbed through after a merge.

---

## Test

`ScrubTest` was updated to call `performScrub(cfs, false, true, true)` with `offline=true`, exercising the path where the primary index file has been deleted.
