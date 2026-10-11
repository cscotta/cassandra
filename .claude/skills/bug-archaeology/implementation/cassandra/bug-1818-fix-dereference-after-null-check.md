# Bug 1818: Dereference After Null Check in SSTableMetadataViewer

**Commit**: 6e6730d875 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/tools
**Category**: Missing null / bounds check

---

## Root Cause

In `SSTableMetadataViewer`, `printHistograms(stats, out)` was called outside the `if (stats != null)` block. The null-check was present to guard all access to `stats`, but the histogram-printing call had been placed after the closing brace of that block, meaning it would execute (and NPE) whenever `stats` was null.

### Before / After

```java
// BEFORE
            if (stats != null)
            {
                out.printf("...", stats.getEstimatedDroppableTombstoneRatio(...));
                out.println(stats.replayPosition);
            }
            printHistograms(stats, out);  // called even when stats == null
```

```java
// AFTER
            if (stats != null)
            {
                out.printf("...", stats.getEstimatedDroppableTombstoneRatio(...));
                out.println(stats.replayPosition);
                printHistograms(stats, out);  // safely inside null guard
            }
```

---

## How to Spot

When a method call on an object appears immediately after a null-guarding block's closing brace, and the method accepts that same object as a parameter, it is almost certainly a brace-placement error. Review tools that highlight "call after null check exits scope" catch this pattern.

---

## Test

No test added. The fix is a one-line brace-placement correction in a command-line tool; the failure mode (NPE or garbled output when reading corrupted/missing sstable metadata) is user-visible but not regression-tested in this commit.
