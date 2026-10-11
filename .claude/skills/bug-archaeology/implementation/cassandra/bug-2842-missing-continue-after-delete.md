# Bug 2842: Missing continue after deleting compacted SSTable causes double-processing

**Commit**: 440f0399b5 · **Ticket**: n/a (regression fix for CASSANDRA-1736)

## Detection Rule
When an `if` block deletes a file and the surrounding loop should skip further processing of that entry, add an explicit `continue` statement; without it, subsequent code in the same loop iteration will attempt to open a file that no longer exists.

## Code Smell
```java
if (components.contains(Component.COMPACTED_MARKER) || desc.temporary)
    SSTable.delete(desc, components);
File dataFile = new File(desc.filenameFor(Component.DATA));
if (components.contains(Component.DATA) && dataFile.length() > 0)
    // opens the just-deleted file
```
The missing `continue` causes the loop to fall through into file-opening logic for an already-deleted SSTable.

## Tags
- scope: cassandra
- category: state-not-cleaned
- code-pattern: missing-continue-after-delete
- severity: crash
- subsystem: storage-engine

## Root Cause
A prior commit (2843) refactored `conditionalDelete` to `delete` and removed the early `return false` guard. Without the `continue`, the startup SSTable scanning loop would try to open data files that had just been deleted, causing `IOError` or silently adding ghost SSTables to the live set.

## Example
```java
// BEFORE
if (components.contains(Component.COMPACTED_MARKER) || desc.temporary)
    SSTable.delete(desc, components);
// falls through to file-open code below

// AFTER
if (components.contains(Component.COMPACTED_MARKER) || desc.temporary)
{
    SSTable.delete(desc, components);
    continue;
}
```
