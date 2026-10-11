# Bug 2795: RandomPartitioner describeOwnership Computes Wrong Range for Non-Wrapping Segments

**Commit**: 2ed36c6ad7 · **Ticket**: CASSANDRA-2071

## Detection Rule
When computing token range ownership percentages, if the formula for intermediate (non-wrapping) ranges does not apply modular arithmetic, negative subtraction results when `T(i-1) > T(i)` wrap around the ring, producing negative or incorrect ownership values.

## Code Smell
The loop body computes `ti.subtract(tim1)` (raw difference) without `add(ri).mod(ri)`, so ranges that cross the ring's maximum boundary produce negative `BigDecimal` values that are then divided to yield a negative or zero percentage.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: missing-modular-ring-arithmetic
- severity: silent-wrong-result
- subsystem: tools-clients

## Root Cause
`RandomPartitioner.describeOwnership()` iterates sorted tokens and computes each token's range size as the difference from the previous token. For the start token's wrap-around range the formula already used `add(ri).mod(ri)`, but the inner loop for all other tokens used bare subtraction `ti.subtract(tim1)`. In a sorted token list a token's predecessor always has a smaller value, so the subtraction is always positive — but the fix adds `add(ri).mod(ri)` to handle edge cases and be consistent. The real bug was that the start token's formula was applied only once outside the loop, while a comment described the wrong algorithm. The fix applies the modular formula uniformly inside the loop for all intermediate tokens.

## Example
```java
// BEFORE: missing modular arithmetic in loop
while (i.hasNext()) {
    t = (Token)i.next(); ti = ((BigIntegerToken)t).token;
    float x = new BigDecimal(ti.subtract(tim1)).divide(r).floatValue(); // no mod
    ownerships.put(t, x);
    tim1 = ti;
}
```
```java
// AFTER: consistent modular arithmetic
while (i.hasNext()) {
    t = (Token)i.next(); ti = ((BigIntegerToken)t).token;
    float x = new BigDecimal(ti.subtract(tim1).add(ri).mod(ri)).divide(r).floatValue();
    ownerships.put(t, x);
    tim1 = ti;
}
```
