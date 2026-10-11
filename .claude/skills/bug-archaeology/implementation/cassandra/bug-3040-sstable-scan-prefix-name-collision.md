# Bug 3040: SSTable file scan matches column families whose names are prefixes of others

**Commit**: db2d62c22a · **Ticket**: CASSANDRA-3040

## Detection Rule
When filtering filenames by column family name prefix, append the separator character to the prefix to avoid false matches against column families whose names are prefixes of other column family names.

## Code Smell
`file.getName().startsWith(columnFamilyName)` will match files belonging to `ColumnFamilyFoo` when scanning for `ColumnFamily` because the name is a pure prefix without requiring the separator.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: startswith-prefix-without-separator
- severity: crash
- subsystem: storage-engine

## Root Cause
`ColumnFamilyStore.files()` and `SSTable.componentsFor()` filtered files using `file.getName().startsWith(columnFamilyName)`. If two column families shared a name where one was a prefix of the other (e.g. `CF` and `CF2`), files for `CF2` would be incorrectly included when scanning for `CF`. The fix appends `Component.separator` (`'-'`) to form `sstableFilePrefix`, ensuring the match requires the separator after the CF name.

## Example
```java
// BEFORE
if (file.isDirectory() || !file.getName().startsWith(columnFamilyName))
    return false;
```
```java
// AFTER
final String sstableFilePrefix = columnFamilyName + Component.separator;
if (file.isDirectory() || !file.getName().startsWith(sstableFilePrefix))
    return false;
```
