# Bug 0972: Infinite loop in Splitter when owned range is smaller than data directory count

**Commit**: 8bc8fa3690 · **Ticket**: CASSANDRA-11034
**Subsystem**: DHT / data distribution
**Category**: Logic error in condition

---

## Root Cause
`Splitter.splitOwnedRanges()` divides the total token range by the desired number of parts to compute a `perPart` token budget. When the owned token range is very small — smaller than the number of configured data directories — integer division produces `perPart == 0`. All subsequent splitting and range-walking logic assumed a positive `perPart`, so the loop never advanced, causing the node to spin indefinitely.

The fix adds an early-exit guard: if `perPart` is zero, the method returns a single-element list containing the partitioner's maximum token, indicating the range cannot be meaningfully split.

### Before / After
```java
// BEFORE
BigInteger perPart = totalTokens.divide(BigInteger.valueOf(parts));

if (dontSplitRanges)
    return splitOwnedRangesNoPartialRanges(localRanges, perPart, parts);

// AFTER
BigInteger perPart = totalTokens.divide(BigInteger.valueOf(parts));
// the range owned is so tiny we can't split it:
if (perPart.equals(BigInteger.ZERO))
    return Collections.singletonList(partitioner.getMaximumToken());

if (dontSplitRanges)
    return splitOwnedRangesNoPartialRanges(localRanges, perPart, parts);
```

---

## How to Spot
Any loop or iteration that advances by a computed step derived from integer division should guard against a zero divisor result. The pattern `BigInteger step = total.divide(count); loop using step` is hazardous when `total < count` is possible at runtime. Reviews should ask: what happens when the step is zero? If the answer is "infinite loop", a zero-guard is mandatory.

---

## Test
No test added.
