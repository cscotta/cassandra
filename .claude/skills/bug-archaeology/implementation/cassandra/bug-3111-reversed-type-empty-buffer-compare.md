# Bug 3111: ReversedType compare inverts empty-buffer sentinel value

**Commit**: 32182f09e1 · **Ticket**: CASSANDRA-3111

## Detection Rule
When a `compare()` method negates the delegate's result without guarding against empty-ByteBuffer sentinels, verify it handles the empty-is-minimum contract before delegating.

## Code Smell
A `compare()` method that simply returns `-baseType.compare(o1, o2)` without first checking `o1.remaining() == 0` or `o2.remaining() == 0` will invert the ordering of the empty-buffer sentinel.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: negated-comparison-missing-sentinel
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
`ReversedType.compare` negated its base type's comparison, which relies on the base type returning a predictable signed value for empty buffers. When both buffers are empty the negation of 0 is still 0, but when one buffer is empty and the base returns -1, negation yields +1 — reversing what should still be the smallest possible value. The fix adds early-return guards that enforce "empty is always smaller" before ever calling the base comparator.

## Example
```java
// BEFORE
public int compare(ByteBuffer o1, ByteBuffer o2)
{
    return -baseType.compare(o1, o2);
}
```
```java
// AFTER
public int compare(ByteBuffer o1, ByteBuffer o2)
{
    if (o1.remaining() == 0)
        return o2.remaining() == 0 ? 0 : -1;
    if (o2.remaining() == 0)
        return 1;
    return -baseType.compare(o1, o2);
}
```
