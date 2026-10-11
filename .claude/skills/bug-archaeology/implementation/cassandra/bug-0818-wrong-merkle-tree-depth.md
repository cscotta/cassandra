# Bug 0818: Wrong Merkle Tree Depth Calculation

**Commit**: c70ce6307d · **Ticket**: CASSANDRA-12580
**Subsystem**: Repair / Anti-Entropy
**Category**: Logic error in condition

---

## Root Cause
The Merkle tree depth used during repair validation was computed as `floor(log_e(numPartitions))` — natural logarithm floored — instead of the correct `ceil(log_2(numPartitions))` — base-2 logarithm ceiling. The natural log grows much more slowly than base-2, so for a table with, say, 1 000 partitions the old formula yielded depth 6 (ln 1000 ≈ 6.9) while the correct answer is 10 (log₂ 1000 ≈ 9.97 → 10). An under-deep tree means each leaf covers a wider token range, reducing repair resolution and potentially missing divergent data.

### Before / After
```java
// BEFORE — natural log, floored
int depth = numPartitions > 0
    ? (int) Math.min(Math.floor(Math.log(numPartitions)), 20)
    : 0;

// AFTER — base-2 log, ceiling
int depth = numPartitions > 0
    ? (int) Math.min(Math.ceil(Math.log(numPartitions) / Math.log(2)), 20)
    : 0;
```

---

## How to Spot
Depth formulas for binary tree structures must use log base-2. Using `Math.log()` (natural log) in contexts where a power-of-two calculation is required is a classic mistake — review any formula that passes its result to a power-of-2 construction (`new MerkleTree(... (int) Math.pow(2, depth))`) and verify the log base matches.

---

## Test
`ValidatorTest` was substantially expanded: it now drives the validator through a configurable number of simulated partitions and verifies that the resulting tree has the expected depth and even distribution. `MerkleTree.rowCount()` — a helper added in the same commit — enables the test to inspect how many rows ended up in each leaf range.
