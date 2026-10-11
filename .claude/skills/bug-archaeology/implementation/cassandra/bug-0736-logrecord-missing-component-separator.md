# Bug 0736: LogRecord absolute path missing component separator causes data loss on 2.1-to-3.0 upgrade

**Commit**: 1ba68a1e5d · **Ticket**: CASSANDRA-13294
**Subsystem**: SSTable lifecycle / log transactions
**Category**: Logic error in condition

---

## Root Cause
`LogRecord.make()` built the absolute path by calling `table.descriptor.baseFilename()`, which for pre-3.0 (2.1) SSTables does not include the component separator character (`-`) after the generation number. When `LogFile` scanned all files to match against a log record it used `String.startsWith(absolutePath)`, so an SSTable with generation `10` would incorrectly match as a file belonging to generation `1` — causing those SSTables to be deleted during transaction replay and producing data loss on upgrade.

The fix appends `Component.separator` (`-`) to the path both when constructing a new record and when parsing an existing one, while stripping the extra character before writing to disk so that existing txn files remain backward-compatible with their stored checksums.

### Before / After
```java
// BEFORE
String absoluteTablePath = FileUtils.getCanonicalPath(table.descriptor.baseFilename());

// AFTER
// Add separator so that "mc-1-" never prefix-matches "mc-10-"
String absoluteTablePath = FileUtils.getCanonicalPath(table.descriptor.baseFilename() + Component.separator);
```

---

## How to Spot
Any path-prefix matching logic that compares numeric suffixes should include the delimiter that follows the number. In review, look for `startsWith` or `contains` calls on SSTable base paths and confirm a non-numeric terminator (separator, dot, or slash) is included in the pattern to prevent generation-number prefix collisions.

---

## Test
`LogTransactionTest` was updated to cover the upgrade path where 2.1-style filenames (no separator) are mixed with 3.0-style filenames, verifying that no false-positive file matches occur.
