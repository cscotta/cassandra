# Bug 1768: Fix EstimatedHistogram Races

**Commit**: 16f99c5a2e · **Ticket**: CASSANDRA-6682
**Subsystem**: src/java/org/apache/cassandra/utils/EstimatedHistogram.java
**Category**: Race condition / missing synchronization

---

## Root Cause

`EstimatedHistogram` used `AtomicLongArray` for the bucket counters but had two separate race windows. First, `getBuckets(reset=true)` read all values then reset them in a second pass, so counts accumulated between the two loops were silently lost. Second, `mean()` called `buckets.get(i)` twice for the same index — once to accumulate `elements` and once to accumulate `sum` — so the two reads could observe different values and produce a nonsensical mean. Additionally, `makeOffsets` was an instance method that mutated a non-final field, making it unsafe to publish to other threads without an explicit happens-before edge.

### Before / After

```java
// BEFORE – read all, then zero all (window between two loops)
long[] rv = new long[buckets.length()];
for (int i = 0; i < buckets.length(); i++)
    rv[i] = buckets.get(i);
if (reset)
    for (int i = 0; i < buckets.length(); i++)
        buckets.set(i, 0L);

// mean() reads the same slot twice
elements += buckets.get(i);
sum += buckets.get(i) * bucketOffsets[i];
```

```java
// AFTER – atomic swap captures the value being returned
if (reset)
    for (int i = 0; i < len; i++)
        rv[i] = buckets.getAndSet(i, 0L);
else
    for (int i = 0; i < len; i++)
        rv[i] = buckets.get(i);

// mean() reads slot once, reuses the value
long bCount = buckets.get(i);
elements += bCount;
sum += bCount * bucketOffsets[i];
```

---

## How to Spot

Any code that reads an atomic value twice in order to perform two related operations on it is a candidate for this bug. Reviewers should look for double-read patterns on `AtomicLong`/`AtomicLongArray` cells and for reset loops that are separate from read loops — both indicate a missing `getAndSet`.

---

## Test

No test added.
