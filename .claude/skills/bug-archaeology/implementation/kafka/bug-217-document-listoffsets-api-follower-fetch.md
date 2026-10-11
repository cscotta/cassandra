**Commit**: 7f9f34a711 · **Ticket**: KAFKA-19649
**Subsystem**: docs/getting-started
**Category**: Logic error in condition

**Root cause**
Document features introduced via KIP-1023. Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  * A new dynamic broker configuration `follower.fetch.last.tiered.offset.enable` (default: `false`) has been added. When enabled on a cluster with tiered storage, a newly added follower replica that has no local data will skip directly to the earliest pending upload offset on the leader, avoiding re-fetching data that is already stored in remote storage. This reduces bootstrap time significantly for large tiered-storage topics. For further details, please refer to [KIP-1023](https://cwiki.apache.org/confluence/x/8op3EQ).
  * The `ListOffsets` API has been extended to version 11, adding support for the `EARLIEST_PENDING_UPLOAD_TIMESTAMP` (-6) timestamp type. This allows clients to query the earliest offset on the leader that has not yet been uploaded to tiered storage. For further details, please refer to [KIP-1023](https://cwiki.apache.org/confluence/x/8op3EQ).
```

**How to spot**
When reviewing docs/getting-started code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19649: document listoffsets api v11 and follower fetch from tiered of… (#22038).

**Test**
No test added.
