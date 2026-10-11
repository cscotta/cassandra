# Bug 2400: Array equality checked with == instead of length-based content check in Hadoop reader

**Commit**: 0c2d2ccd96 · **Ticket**: CASSANDRA-3976

## Detection Rule

When code uses `==` to compare a byte array against a known empty-array sentinel, it will silently return the wrong result for any logically-empty array that is a different object instance (e.g. `new byte[0]` or a freshly deserialized empty array).

## Code Smell

`start == ArrayUtils.EMPTY_BYTE_ARRAY` — reference equality comparison against a sentinel constant; any independently-allocated empty byte array will fail the check and cause the predicate to incorrectly report a non-default slice.

## Tags

- scope: cassandra
- category: logic-error
- code-pattern: reference-equality-on-array
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause

`ColumnFamilyRecordReader.isSliceRange()` checked whether a slice predicate's start/finish were "empty" by comparing references: `start == ArrayUtils.EMPTY_BYTE_ARRAY`. When the deserialized byte arrays were not the singleton constant — even if they had zero length — the comparison returned false, making the method incorrectly signal a non-default slice range. The fix tests `start.length > 0` (and `finish.length > 0`), which is correct regardless of object identity.

## Example

```java
// BEFORE
if ((start == null || start == ArrayUtils.EMPTY_BYTE_ARRAY) &&
    (finish == null || finish == ArrayUtils.EMPTY_BYTE_ARRAY))
    return true;  // wrong: misses new byte[0] instances
return false;
```

```java
// AFTER
if ((start != null) && (start.length > 0))
    return false;
if ((finish != null) && (finish.length > 0))
    return false;
return true;
```
