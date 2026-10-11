**Commit**: c9cfaafac4 · **Ticket**: KAFKA-20331
**Subsystem**: docs/getting-started
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
  * A new `group.coordinator.background.threads` config has been added to control the size of the group coordinator's thread pool used to updating regular expression subscriptions. Previously, the thread pool only had a single thread. Now the thread pool has two threads by default and its size is configurable. For further details, please refer to [KIP-1263](https://cwiki.apache.org/confluence/x/DIE8G).
  * New `group.consumer.assignment.interval.ms`, `group.share.assignment.interval.ms` and `group.streams.assignment.interval.ms` configs have been added to set the interval between assignment updates for consumer, share and streams groups, along with broker configs for specifying minimum and maximum values and group configs. These default to an interval of 1 second and previously had an effective value of 0. For further details, please refer to [KIP-1263](https://cwiki.apache.org/confluence/x/DIE8G).
```

**How to spot**
When reviewing docs/getting-started code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20331, kafka-20240: update upgrade.md for new kip-1263 configs in 4.3 (#21822).

**Test**
No test added.
