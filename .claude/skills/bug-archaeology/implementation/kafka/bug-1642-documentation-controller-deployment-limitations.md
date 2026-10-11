**Commit**: c0b3e12dcf · **Ticket**: KAFKA-18330
**Subsystem**: docs/ops.html
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
    <li>For redundancy, a Kafka cluster should use 3 or 5 controllers, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. In the rare case of a partial network failure it is possible for the cluster metadata quorum to become unavailable. This limitation will be addressed in a future release of Kafka.</li>

// After
    <li>For redundancy, a Kafka cluster should use 3 or more controllers, depending on factors like cost and the number of concurrent failures your system should withstand without availability impact. For the KRaft controller cluster to withstand <code>N</code> concurrent failures the controller cluster must include <code>2N + 1</code> controllers.</li>
```

**How to spot**
When reviewing docs/ops.html code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18330: update documentation to remove controller deployment limitations (#18281).

**Test**
No test added.
