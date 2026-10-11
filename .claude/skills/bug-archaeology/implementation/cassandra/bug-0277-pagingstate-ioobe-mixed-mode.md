# Bug 0277: IndexOutOfBoundsException in PagingState Format Detection on Mixed-Mode Clusters

**Commit**: ab481be81d · **Ticket**: CASSANDRA-17840
**Subsystem**: service/pager/PagingState
**Category**: Missing null / bounds check

---

## Root Cause

`isModernSerialized()` computed byte-buffer index offsets by accumulating `long` values (VInt
sizes and field lengths) into an `int` local variable using plain addition.  For pathologically
large or adversarially crafted field lengths, the accumulation could silently overflow into a
negative integer.  A negative `index` was then passed to `getUnsignedVInt()`, which performs a
`ByteBuffer` array access using that index and throws `IndexOutOfBoundsException`.  In a
mixed-mode cluster, one node serializes in legacy format and the other attempts to parse it as
modern; the heuristic probe is fed arbitrary bytes, making overflow reachable in practice.

### Before / After

```java
// BEFORE – plain addition, no overflow check
index += computeUnsignedVIntSize(partitionKeyLen) + partitionKeyLen;
if (index >= limit)
    return false;
```

```java
// AFTER – sentinel-returning helper prevents negative index propagation
index = addNonNegative(index, computeUnsignedVIntSize(partitionKeyLen), partitionKeyLen);
if (index >= limit || index < 0)
    return false;

private static int addNonNegative(int x, int y, int z)
{
    int sum = x + y;
    if (sum < 0) return -1;
    sum += z;
    if (sum < 0) return -1;
    return sum;
}
```

---

## How to Spot

Any code that accumulates VInt-encoded lengths into an `int` offset while parsing untrusted or
cross-version byte buffers must guard against integer overflow at each step.  The pattern
"index += vint_size + field_len" is particularly dangerous because both terms can be large and
positive while their sum wraps negative.  During review, flag all buffer-offset arithmetic in
deserialization probes that reads user-supplied length fields and confirm that overflow is
either impossible by construction or is explicitly detected and handled before the next
dereference.

---

## Test

`RandomizedPagingStateTest` runs 50,000 rounds serializing `PagingState` objects with random
partition-key and clustering-key payloads and asserts that `isLegacySerialized` returns `true`
and `isModernSerialized` returns `false` for every produced buffer without throwing.
