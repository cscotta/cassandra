# Bug 2249: Murmur3Partitioner MINIMUM Token Set to 0 Instead of Long.MIN_VALUE

**Commit**: 93685a4780 · **Ticket**: CASSANDRA-4621
**Subsystem**: src/java/org/apache/cassandra/dht/Murmur3Partitioner.java
**Category**: Wrong constant / default value

---

## Root Cause

`Murmur3Partitioner.MINIMUM` was defined as `new LongToken(0L)`, which caused an assertion failure in `getRestrictedRange`. The correct sentinel for the ring's minimum token is `Long.MIN_VALUE` — the value that represents the open left endpoint of the full token ring. With `MINIMUM = 0`, any key that hashed to a negative long was treated as less than the minimum, breaking range boundary assumptions.

Additionally, the original `getToken` implementation tried to make all tokens non-negative by flipping the sign of negative hashes, which is a lossy transformation that produces duplicates and invalidates the full-range coverage guarantee. The fix instead maps the single excluded value (`Long.MIN_VALUE`, which equals `MINIMUM`) to `Long.MAX_VALUE`, preserving the full 64-bit hash space while ensuring no real key can produce a token equal to `MINIMUM`.

### Before / After

```java
// BEFORE
public static final LongToken MINIMUM = new LongToken(0L);
...
long hash = MurmurHash.hash3_x64_128(...)[0];
return new LongToken((hash < 0) ? -hash : hash);  // lossy, no negatives
```

```java
// AFTER
public static final LongToken MINIMUM = new LongToken(Long.MIN_VALUE);
...
long hash = MurmurHash.hash3_x64_128(...)[0];
return new LongToken(normalize(hash));  // excludes only MIN_VALUE

private long normalize(long v) {
    return v == Long.MIN_VALUE ? Long.MAX_VALUE : v;
}
```

---

## How to Spot

When a ring-based partitioner defines a MINIMUM sentinel, verify it matches the actual type's minimum value, not an arbitrary value like `0`. Any range arithmetic that does `(MINIMUM, X]` to cover all tokens below X will silently mis-classify tokens that fall between the wrong sentinel and `0`.

---

## Test

No test added.
