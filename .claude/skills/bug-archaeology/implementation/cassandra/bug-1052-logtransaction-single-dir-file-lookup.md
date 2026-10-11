# Bug 1052: LogTransaction Checks Only One Directory for SSTable Files

**Commit**: 73781a9a49 · **Ticket**: CASSANDRA-10421
**Subsystem**: Storage Engine / Lifecycle / Transaction Log
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
`LogTransaction` tracked SSTable files using relative paths anchored to a single parent directory — the directory chosen at construction time via `Directories.getDirectoryForNewSSTables()`. When verifying or cleaning up log records, `LogRecord.getExistingFiles()` called `parentFolder.listFiles(...)` with a filter based on the stored relative filename. If an SSTable's data files were spread across multiple data directories (JBOD or tiered-storage configurations), the lookup only scanned one directory and missed files in others, potentially leaving orphan files or incorrectly treating live SSTables as absent after a crash.

The fix converts all path tracking to absolute canonical paths. `LogRecord` now stores `Optional<String> absolutePath` instead of `relativeFilePath`, and `getExistingFiles` derives the parent directory from the absolute path itself rather than relying on an externally supplied folder. The `LogFile` is refactored into `LogReplica`/`LogReplicaSet` classes that mirror the log file across all relevant directories so that every directory containing an SSTable also holds a copy of the relevant log record, making crash recovery directory-local.

### Before / After
```java
// BEFORE – file listing anchored to single construction-time folder
public List<File> getExistingFiles(File folder)
{
    return Arrays.asList(folder.listFiles((dir, name) -> name.startsWith(relativeFilePath)));
}

// AFTER – absolute path drives the listing; any directory can be found
public static List<File> getExistingFiles(String absoluteFilePath)
{
    Path path = Paths.get(absoluteFilePath);
    File[] files = path.getParent().toFile()
                       .listFiles((dir, name) -> name.startsWith(path.getFileName().toString()));
    return files == null ? Collections.emptyList() : Arrays.asList(files);
}
```

---

## How to Spot
In any component that stores a relative file path for later lookup, ask whether the working directory or base folder might differ at lookup time from what was assumed at write time. For SSTable lifecycle operations specifically, always use canonical absolute paths because data directories are configurable per-node and can be many. Any `listFiles` call that takes a caller-supplied base folder rather than deriving it from the file path itself is a red flag in a multi-directory storage system.

---

## Test
`LogTransactionTest` was heavily expanded (from ~150 to ~575 lines) with tests covering multi-directory scenarios, replica verification, and crash-recovery cleanup paths. `RealTransactionsTest` was also updated.
