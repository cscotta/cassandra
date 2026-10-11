**Commit**: f7aaf46a14 · **Ticket**: KAFKA-19772
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Clarify the Javadoc for `Node#isFenced` to align with KIP-1073, which
introduced the “fenced” field in `DescribeCluster` for brokers. The
“fenced” flag applies only to broker nodes returned by
`DescribeCluster`. For controller quorum nodes, it doesn’t apply and is
always `false`. This clarifies that not every node has a meaningful
fenced state.

**Before/After**
```java
// Before
     * Whether if this node is fenced

// After
     * Returns whether this node is fenced.
     * <p>
     * This applies to broker nodes only. For controller quorum nodes, this field
     * is not relevant and is defined to be {@code false}.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19772 enhance the documentation for `node#isfanced` (#20689).

**Test**
No test added.
