# Bug 1321: Top Partitions Reporting Wrong Cardinality

**Commit**: 0127b6963e · **Ticket**: CASSANDRA-8834
**Subsystem**: utils/TopKSampler, db/ColumnFamilyStore
**Category**: Wrong constant / default value

---

## Root Cause

`TopKSampler` was feeding raw Java object references into the HyperLogLog estimator via `hll.offer(item)`, which hashes the object using its Java identity hash code. Because `ByteBuffer` objects representing the same key do not share an identity, different `ByteBuffer` instances wrapping the same bytes could hash to different buckets in HLL, inflating the estimated cardinality. The fix pre-computes a stable Murmur3 hash of the key bytes and passes it to `hll.offerHashed(hash)`, so the estimator sees a consistent value regardless of object identity.

### Before / After

```java
// BEFORE
public void addSample(final T item, final int value) {
    ...
    summary.offer(item, value);
    hll.offer(item);           // uses object identity hash — wrong for ByteBuffer
}
```

```java
// AFTER
public void addSample(final T item, final long hash, final int value) {
    ...
    summary.offer(item, value);
    hll.offerHashed(hash);     // uses pre-computed content hash — correct
}
```

---

## How to Spot

Whenever a cardinality or distinct-count estimator (HLL, MinHash, etc.) is offered a mutable or value-based object, verify that the estimator is using content equality rather than reference equality for hashing. Any call to `hll.offer(obj)` where `obj` is a `ByteBuffer`, array, or other type that overrides `equals` but not `hashCode` in a content-aware way should be treated as suspect. The correct pattern is always to compute a stable hash of the logical key and call `offerHashed`.

---

## Test

`TopKSamplerTest` was updated to pass explicit `MurmurHash.hash64` values and now asserts `sampler2.hll.cardinality() == 10`, directly verifying that distinct-key counting is accurate.
