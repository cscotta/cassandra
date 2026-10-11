# Bug 0247: Wrong Paging State Encoding for Compact Storage with Single Clustering Column

**Commit**: 9a0af4112e · **Ticket**: CASSANDRA-17507
**Subsystem**: service/pager/PagingState
**Category**: Wrong serialization / deserialization

---

## Root Cause

`PagingState.LegacyPagingState` encodes and decodes clustering keys using the pre-3.0 composite cell-name format when serializing paging state for native protocol v3. For tables with `COMPACT STORAGE` and exactly one clustering column, CQL protocol v3 (as implemented in 3.x nodes) does not wrap the clustering value in a composite; it transmits the raw bytes directly. The 4.x implementation of `encodeCellName` and `decodeCellName` always built a composite, so when a 4.x node returned a paging token and a 3.x node tried to resume from it (or vice versa during a rolling upgrade), the clustering boundary was decoded incorrectly, causing the next page to start at the wrong row or to fail entirely. The fix short-circuits both encode and decode paths when `metadata.isCompactTable() && metadata.comparator.size() == 1`: encode returns `clustering.bufferAt(0)` (raw bytes) and decode returns `Clustering.make(value)` directly, matching the 3.x wire format.

### Before / After

```java
// BEFORE – encodeCellName always builds a composite:
boolean isStatic = clustering == Clustering.STATIC_CLUSTERING;
// ... composite assembly ...

// BEFORE – decodeCellName always unwraps a composite:
if (CompositeType.isStaticName(value, ...)) return Clustering.STATIC_CLUSTERING;
```

```java
// AFTER – short-circuit for single-column compact tables:
if (metadata.isCompactTable() && metadata.comparator.size() == 1)
    return clustering.bufferAt(0); // encode

if (metadata.isCompactTable() && metadata.comparator.size() == 1)
    return Clustering.make(value); // decode
```

---

## How to Spot

Protocol compatibility bugs in paging state are subtle because the paging token is opaque to clients. Review every encode/decode path in `LegacyPagingState` for special-cased table shapes (compact, static-only, no-clustering) and verify the encoding matches what older protocol versions actually sent. Unit tests for paging that only use standard CQL tables will miss compact-storage edge cases; explicit compact-storage tests for each protocol version are required.

---

## Test

New upgrade dtests `CompactStoragePagingWithProtocolV30Test` and `CompactStoragePagingWithProtocolV3XTest` set fetch-size=1 on a compact-storage table in a mixed 3.x/4.x cluster and assert all rows are returned correctly with protocol v3, v4, and v5.
