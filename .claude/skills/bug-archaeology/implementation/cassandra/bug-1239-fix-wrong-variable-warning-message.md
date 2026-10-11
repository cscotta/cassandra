# Bug 1239: Fix Wrong Variable in Warning Message

**Commit**: 5a571c6291 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/compaction/Scrubber.java
**Category**: Wrong constant / default value

---

## Root Cause

In `Scrubber`, a warning message comparing the data file row position against the index file row position used the wrong variable as the second format argument. The format string `"Data file row position %d differs from index file row position %d"` was passed `dataStart` and `dataSizeFromIndex`, but `dataSizeFromIndex` is a size, not a position. The correct variable is `dataStartFromIndex`, which represents the index-recorded starting position. The bug caused misleading output in the warning log, reporting a size value in a context where an offset was expected.

### Before / After

```java
// BEFORE
outputHandler.warn(String.format(
    "Data file row position %d differs from index file row position %d",
    dataStart, dataSizeFromIndex));
```

```java
// AFTER
outputHandler.warn(String.format(
    "Data file row position %d differs from index file row position %d",
    dataStart, dataStartFromIndex));
```

---

## How to Spot

Format strings that contain multiple `%d` placeholders with semantically similar but distinct variable names are prone to copy-paste or transposition errors. When reviewing log/warning formatting code, verify each positional argument against the corresponding slot in the format string by name and type. Variables sharing a prefix (e.g., `dataStart` vs `dataStartFromIndex` vs `dataSizeFromIndex`) are particularly easy to confuse.

---

## Test

No test added.
