# Bug 2790: UUIDv1 Generation Uses Unsafe Timestamp for Time-Safe Callers

**Commit**: 4921acdb32 · **Ticket**: n/a

## Detection Rule
When a UUIDv1 generation method is intended to produce globally unique values (no duplicates within the same JVM) but passes a caller-supplied millisecond timestamp directly into the non-atomic monotonic counter logic, concurrent calls with the same timestamp can produce the same UUID.

## Code Smell
`makeType1UUIDFromHost` and `getTimeUUIDBytes()` (zero-arg) both called `createTime(System.currentTimeMillis())`. The `createTime` method had a monotonic bump, but `getTimeUUIDBytes(long timeMillis)` bypassed it entirely and called the same `createTime(timeMillis)` with an external timestamp without incrementing `lastNanos`.

## Tags
- scope: java
- category: race-condition
- code-pattern: non-monotonic-uuid-timestamp
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`UUIDGen.createTime(long when)` used `when` as a raw epoch-ms and applied a monotonic guard (`lastNanos`) only in the synchronized path. When `getTimeUUIDBytes(long timeMillis)` was called by multiple threads with the same `timeMillis`, each would produce the same `nanosSince` and the same UUID because `lastNanos` wasn't bumped (the guard compared `nanosSince > lastNanos` but with the same input it was always equal to `lastNanos` only after the first call — subsequent calls within the same ms from the same timestamp would produce duplicates). The fix splits the method into `createTimeSafe()` (always uses `System.currentTimeMillis()` with the monotonic guard) and `createTimeUnsafe(long when)` (explicit no-uniqueness-guarantee), while the default `getTimeUUIDBytes()` now uses the safe path.

## Example
```java
// BEFORE: zero-arg uses currentTimeMillis but same code path as unsafe variant
public static byte[] getTimeUUIDBytes() {
    return getTimeUUIDBytes(System.currentTimeMillis()); // calls unsafe path
}
public static byte[] getTimeUUIDBytes(long timeMillis) {
    long msb = instance.createTime(timeMillis), lsb = instance.getClockSeqAndNode();
    // ... duplicates possible with same timeMillis
}
```
```java
// AFTER: safe vs unsafe split
public static byte[] getTimeUUIDBytes() {
    return createTimeUUIDBytes(instance.createTimeSafe()); // monotonic, unique
}
public static byte[] getTimeUUIDBytes(long timeMillis) {
    return createTimeUUIDBytes(instance.createTimeUnsafe(timeMillis)); // documented as non-unique
}
```
