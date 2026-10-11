**Commit**: f7e64d3fee · **Ticket**: KAFKA-20290
**Subsystem**: docs/getting-started
**Category**: Logic error in condition

**Root cause**
Add a line to upgrade.md for KIP-1240. Reviewers: Lan Ding <isDing_L@163.com>, majialong
 <majialoong@gmail.com>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
  * New group configs have been introduced: `share.delivery.count.limit`, `share.partition.max.record.locks` and `share.renew.acknowledge.enable`, along with equivalent broker configs for specifying minimum and maximum values. In addition, the validation of group configs has been improved. For further details, please refer to [KIP-1240](https://cwiki.apache.org/confluence/x/tIHMFw).
```

**How to spot**
When reviewing docs/getting-started code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20290: kip-1240 updates in upgrade.md (#21703).

**Test**
No test added.
