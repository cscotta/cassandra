**Commit**: a39e714c6f · **Ticket**: KAFKA-20449
**Subsystem**: clients/src/main/java/org/apache/kafka/clients/consumer/internals/OffsetFetcherUtils.java
**Category**: Wrong constant / default value

**Root cause**
`OffsetFetcherUtils.updateSubscriptionState()` logged "Not updating high watermark" and "Not updating last stable offset" at `WARN` level. These messages fire during normal rebalance activity when a `LIST_OFFSETS` response arrives after a partition has already been revoked. The condition is a benign race — no operator action is needed — but `WARN` caused deployments that monitor warning rates (e.g., canary releases) to trigger spurious rollbacks due to the high volume of warnings during rebalances.

**Before/After**
```java
// Before: WARN level for benign race
log.warn("Not updating last stable offset for partition {} as it is no longer assigned", partition);
log.warn("Not updating high watermark for partition {} as it is no longer assigned", partition);

// After: DEBUG level
log.debug("Not updating last stable offset for partition {} as it is no longer assigned", partition);
log.debug("Not updating high watermark for partition {} as it is no longer assigned", partition);
```

**How to spot**
Any log statement inside a "no longer assigned" or "partition revoked" branch should be evaluated for whether it represents an actionable error or a normal transient state. If the condition is an expected side-effect of partition reassignment, `DEBUG` is the correct level; `WARN` should be reserved for conditions that require operator attention.

**Test**
No new test added. Existing `OffsetFetcherUtils` tests were verified to pass; no test asserts on the log level of these messages.
