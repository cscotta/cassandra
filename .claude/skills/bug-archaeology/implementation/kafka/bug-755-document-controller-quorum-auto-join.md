**Commit**: daa7aae0c1 · **Ticket**: KAFKA-19604
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Document controller.quorum.auto.join.enable config in upgrade.html

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <li>
        The <code>controller.quorum.auto.join.enable</code> has been added to <code>QuorumConfig</code>, enabling KRaft controllers to automatically join the cluster's voter set,
        and defaults to false.
        For further details, please refer to <a href="https://cwiki.apache.org/confluence/x/nyH1D">KIP-853</a>.
    </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19604 document controller.quorum.auto.join.enable config in upgrade.html (#20409).

**Test**
No test added.
