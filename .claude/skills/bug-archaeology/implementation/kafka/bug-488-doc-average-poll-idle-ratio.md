**Commit**: f6f2fd89fe · **Ticket**: KAFKA-14664
**Subsystem**: docs/operations
**Category**: Logic error in condition

**Root cause**
The documentation of the average poll idle ratio was changed in
KAFKA-14664, but the ops doc has not yet been updated to reflect the new
documentation. Reviewers: José Armando García Sancio <jsancio@apache.org>

**Before/After**
```java
// Before
The average fraction of time the client's poll() is idle as opposed to waiting for the user code to process records.

// After
The ratio of time the Raft IO thread is idle as opposed to doing work (e.g. handling requests or replicating from the leader)
```

**How to spot**
When reviewing docs/operations code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: update the doc of average poll idle ratio in raft-metrics (#21227).

**Test**
No test added.
