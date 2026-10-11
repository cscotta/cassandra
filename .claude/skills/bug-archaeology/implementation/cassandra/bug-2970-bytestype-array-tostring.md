# Bug 2970: BytesType.getString() calls Object.toString() on byte array

**Commit**: 88b0ee7be4 · **Ticket**: CASSANDRA-338

## Detection Rule
When a `getString(byte[])` method returns `bytes.toString()`, flag it — `byte[].toString()` returns the array's class name and identity hash code (e.g., `[B@1a2b3c`), not the byte content.

## Code Smell
`BytesType.getString(byte[] bytes)` returns `bytes.toString()`, which calls `Object.toString()` on the array reference and produces an unintelligible string like `[B@deadbeef` instead of the actual byte values.

## Tags
- scope: java
- category: wrong-filter-result
- code-pattern: array-toString-instead-of-content
- severity: silent-wrong-result
- subsystem: cql-query

## Root Cause
In Java, arrays do not override `toString()`, so calling it on a `byte[]` returns the JVM's default identity string rather than the array contents. `BytesType.getString()` is used for debug output and CQL display of binary column values. The fix uses `Arrays.toString(bytes)` which formats the array as a readable comma-separated list of byte values.

## Example
```java
// BEFORE: prints "[B@1a2b3c" — useless
public String getString(byte[] bytes) {
    return bytes.toString();
}
```
```java
// AFTER: prints "[72, 101, 108, 108, 111]"
public String getString(byte[] bytes) {
    return Arrays.toString(bytes);
}
```
