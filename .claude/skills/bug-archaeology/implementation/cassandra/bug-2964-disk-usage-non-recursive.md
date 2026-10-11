# Bug 2964: getUsedDiskSpace only counts top-level files, ignores subdirectories

**Commit**: ba88f55d1f · **Ticket**: CASSANDRA-394

## Detection Rule
When computing disk usage by calling `file.length()` on each entry in `listFiles()`, check whether the data directories may have subdirectories — `File.length()` returns 0 or the directory entry size for directories, not the total size of their contents.

## Code Smell
`getUsedDiskSpace()` calls `f.listFiles()` and sums `file.length()` for each entry; any subdirectory in the data directory contributes 0 (or its inode size) rather than the total size of all files inside it.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: non-recursive-disk-usage
- severity: silent-wrong-result
- subsystem: config

## Root Cause
The data directories can contain subdirectories (one per keyspace), each containing SSTable files. The original code listed only the immediate children and summed their lengths, giving a wildly understated disk usage figure when subdirectories were present. The fix introduces a recursive helper `getUsedDiskSpaceForPath` that descends into directories, correctly accumulating file sizes at all levels.

## Example
```java
// BEFORE: non-recursive, misses files inside subdirectories
File[] files = f.listFiles();
for (File file : files) {
    diskSpace += file.length();  // 0 for directories
}
```
```java
// AFTER: recursive traversal
public static long getUsedDiskSpaceForPath(String path) {
    File file = new File(path);
    if (file.isFile()) return file.length();
    long diskSpace = 0;
    for (File childFile : file.listFiles())
        diskSpace += getUsedDiskSpaceForPath(childFile.getPath());
    return diskSpace;
}
```
