# Bug 0314: OVERWRITE Mode Keeps Old Content Past New EOF

**Commit**: 56ec75cc65 · **Ticket**: CASSANDRA-17439
**Subsystem**: src/java/org/apache/cassandra/io/util/PathUtils.java
**Category**: Non-atomic file write

---

## Root Cause

`PathUtils.WRITE_OPTIONS` opened files with only `WRITE | CREATE`, omitting `TRUNCATE_EXISTING`.  This means that when a caller opened an existing file for overwrite and wrote fewer bytes than the file previously contained, the tail of the old content remained on disk beyond the new end of the written data, producing a silently corrupted file with a mix of new data at the start and stale bytes at the end.  Adding `TRUNCATE_EXISTING` to the option set ensures the file is emptied to zero length before the first write.

### Before / After

```java
// BEFORE
private static final Set<StandardOpenOption> WRITE_OPTIONS =
    unmodifiableSet(EnumSet.of(WRITE, CREATE));
```

```java
// AFTER
private static final Set<StandardOpenOption> WRITE_OPTIONS =
    unmodifiableSet(EnumSet.of(WRITE, CREATE, TRUNCATE_EXISTING));
```

---

## How to Spot

Any time a utility opens a file channel with `WRITE | CREATE` but without `TRUNCATE_EXISTING` and without `APPEND`, review whether callers can write to a pre-existing file.  If the new payload is shorter than the original file, silent data corruption occurs.  During code review, treat the three-way split `WRITE/WRITE+APPEND/READ_WRITE` as a strong signal: the non-append write path almost always needs `TRUNCATE_EXISTING` unless the caller explicitly manages file length.

---

## Test

Adds `testOverwrite()` in `FileTest`: writes 100 random bytes to a new file, then overwrites with 50 bytes via `WriteMode.OVERWRITE`, and asserts the file length is exactly 50 and its content matches only the second write.
