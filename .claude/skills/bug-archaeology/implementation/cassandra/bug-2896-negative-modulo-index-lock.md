# Bug 2896: Negative array index from % on negative hashCode in index lock selection

**Commit**: 84aab33341 · **Ticket**: CASSANDRA-1402

## Detection Rule
When using `hashCode() % arrayLength` to select an array slot, verify that the result is wrapped in `Math.abs()` because Java's `%` operator returns a negative value for negative hash codes, causing an `ArrayIndexOutOfBoundsException`.

## Code Smell
`indexLocks[Arrays.hashCode(key) % indexLocks.length]` — `Arrays.hashCode` can return a negative integer; the modulo of a negative integer in Java is also negative, producing an invalid array index.

## Tags
- scope: java
- category: logic-error
- code-pattern: negative-modulo-array-index
- severity: crash
- subsystem: sai-secondary-index

## Root Cause
Java's `%` operator preserves sign, so `(-7) % 5 == -2`. Indexing an array with a negative subscript throws `ArrayIndexOutOfBoundsException`. The fix extracts `indexLockFor(key)` which applies `Math.abs(Arrays.hashCode(key) % indexLocks.length)` to guarantee a non-negative index.

## Example
```java
// BEFORE — can produce negative index
synchronized (indexLocks[Arrays.hashCode(mutation.key()) % indexLocks.length])
```
```java
// AFTER
private Object indexLockFor(byte[] key) {
    return indexLocks[Math.abs(Arrays.hashCode(key) % indexLocks.length)];
}
// ...
synchronized (indexLockFor(mutation.key()))
```
