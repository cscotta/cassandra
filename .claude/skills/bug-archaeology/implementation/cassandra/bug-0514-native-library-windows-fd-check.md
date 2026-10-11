# Bug 0514: NativeLibrary.tryOpenDirectory Returns -1 on Windows, Crashing Callers

**Commit**: 4e6c0fadfe · **Ticket**: CASSANDRA-15426
**Subsystem**: `db/lifecycle/LogReplica.java`, `hints/HintsCatalog.java`
**Category**: Missing null / bounds check

---

## Root Cause

`NativeLibrary.tryOpenDirectory` relies on POSIX `open(2)` semantics, which are not available on Windows; it always returns -1 on that platform. Both `LogReplica.create`, `LogReplica.open`, and `HintsCatalog` treated a -1 return as an unconditional fatal error, throwing `FSReadError` or `FSWriteError` and propagating them to the caller. On Windows this meant every attempt to create a log replica or open the hints directory would fail immediately, making Cassandra completely unbootable. The fix adds a `!FBUtilities.isWindows()` guard before the error is raised, so on Windows the -1 is silently accepted (no directory syncing is performed, which is fine because Windows does not require explicit directory fsync for durability).

### Before / After

```java
// BEFORE (LogReplica.create)
int folderFD = NativeLibrary.tryOpenDirectory(folder.getPath());
if (folderFD == -1)
    throw new FSReadError(new IOException("Invalid folder descriptor..."), ...);
```

```java
// AFTER
int folderFD = NativeLibrary.tryOpenDirectory(folder.getPath());
if (folderFD == -1 && !FBUtilities.isWindows())
    throw new FSReadError(new IOException("Invalid folder descriptor..."), ...);
```

---

## How to Spot

Any call to a POSIX-specific native method where the return value is checked as an error sentinel without first verifying the platform is a potential cross-platform defect. Reviewers should flag patterns of the form `if (nativeResult == SENTINEL_VALUE) throw` whenever the native method is documented as "not supported on Windows" or when the sentinel value is the platform's canonical "not implemented" return. The guard should appear at every call site, or (better) inside the native wrapper itself.

---

## Test

No test added.
