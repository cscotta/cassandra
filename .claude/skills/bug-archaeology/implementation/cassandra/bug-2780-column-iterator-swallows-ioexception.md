# Bug 2780: IOException in Column Iterator Silently Returns null Instead of Propagating

**Commit**: e7edf4a323 · **Ticket**: n/a

## Detection Rule
When an `Iterator.next()` implementation catches a checked exception internally, it must either rethrow it wrapped in an unchecked exception or propagate it; returning `null` silently hides I/O errors from callers.

## Code Smell
`catch (IOException e) { return null; }` in `ColumnIterator.next()` swallows the exception and returns `null` as if it were a valid column, masking read failures.

## Tags
- scope: cassandra
- category: silent-skip
- code-pattern: exception-swallowed-as-null
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`ColumnIterator.next()` in `ColumnSortedMap` caught `IOException` and returned `null`, causing callers to see a spurious null entry in the column map and never learn about the underlying I/O failure. Separately, `setValue()` in the returned `Map.Entry` also returned `null` instead of throwing, silently dropping mutation attempts. Both are fixed by throwing appropriate unchecked exceptions.

## Example
```java
// BEFORE
catch (IOException e) {
    return null;
}
// and
public IColumn setValue(IColumn value) {
    return null;
}
```
```java
// AFTER
catch (IOException e) {
    throw new IOError(e);
}
// and
public IColumn setValue(IColumn value) {
    throw new UnsupportedOperationException();
}
```
