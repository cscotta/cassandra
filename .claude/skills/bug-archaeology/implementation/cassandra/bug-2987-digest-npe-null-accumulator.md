# Bug 2987: ColumnFamily.digest() NPE when column set is empty

**Commit**: fb347bd3d1 · **Ticket**: CASSANDRA-149

## Detection Rule
When computing a digest by XOR-ing byte arrays, initialize the accumulator to an empty array rather than null, and never pass null to XOR utilities.

## Code Smell
A digest accumulator variable is initialized to `null`, and the first-iteration null check is the only guard; if the column set is empty the null propagates to callers as a NullPointerException.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: null-accumulator-empty-collection
- severity: crash
- subsystem: storage-engine

## Root Cause
`ColumnFamily.digest()` initialized `xorHash` to `null` and relied on `xorHash == null` to detect the first element. When the column set was empty the loop never executed and `null` was returned. The caller passed this null to `new ReadResponse(table, digest)` and subsequently to `FBUtilities.xor()`, both of which NPEd. The fix initializes `xorHash` to `ArrayUtils.EMPTY_BYTE_ARRAY` and adds null-assert guards in `xor()` and `ReadResponse`.

## Example
```java
// BEFORE: null accumulator crashes on empty column set
byte[] xorHash = null;
for (IColumn column : columns) {
    if (xorHash == null)
        xorHash = column.digest();
    else
        xorHash = FBUtilities.xor(xorHash, column.digest());
}
return xorHash; // null if columns is empty!

// AFTER: empty-array sentinel is safe
byte[] xorHash = ArrayUtils.EMPTY_BYTE_ARRAY;
for (IColumn column : columns) {
    if (xorHash.length == 0)
        xorHash = column.digest();
    else
        xorHash = FBUtilities.xor(xorHash, column.digest());
}
return xorHash;
```
