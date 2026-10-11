# Bug 1238: Fix Bad Auto-Merge in Scrubber Error Message

**Commit**: f3ca3686d6 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/Scrubber.java
**Category**: Wrong constant / default value

---

## Root Cause

A bad auto-merge left the `IOError` message for an impossible row size as a truncated string `"Impossible row size "` without any contextual description, whereas the intended message from a prior commit was `"Impossible row size (greater than file length): " + dataSize`. The merge conflict resolution dropped the descriptive suffix, making the error message uninformative when thrown during scrubbing.

### Before / After

```java
// BEFORE (post-bad-merge)
throw new IOError(new IOException("Impossible row size " + dataSize));
```

```java
// AFTER
throw new IOError(new IOException("Impossible row size (greater than file length): " + dataSize));
```

---

## How to Spot

Error messages that end with a bare value concatenation (e.g., `"... " + someVar`) and lack a verbal description of the condition are often symptoms of incomplete merge resolutions. When reviewing merge commits, diff each changed file against both parents to verify that the intended message from both branches was preserved.

---

## Test

No test added.
