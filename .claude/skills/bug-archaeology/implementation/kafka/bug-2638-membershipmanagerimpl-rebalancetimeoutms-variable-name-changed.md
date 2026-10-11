**Commit**: 9d634629f2 · **Ticket**: KAFKA-16899
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
As mentioned in the ticket, the config property name "rebalanceTimeoutMs" is misleading since the property only handles the client's commit portion of the process.It is used in MembershipManagerImpl as a means to limit the client's efforts in the case where it is repeatedly trying to commit but failing. Considering the same, the property name has been updated to "commitTimeoutDuringReconciliation" (suggested in ticket) in GroupRebalanceConfig and in all other classes where the property/variable is used

Reviewers: Kirk True <kirk@kirktrue.pro>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
     * Rebalance timeout. To be used as time limit for the commit request issued
    private final int rebalanceTimeoutMs;
                                 int rebalanceTimeoutMs,
            rebalanceTimeoutMs,
                          int rebalanceTimeoutMs,
        this.rebalanceTimeoutMs = rebalanceTimeoutMs;
        // the current reconciliation is in process. Note this is using the rebalance timeout as
        // it is the limit enforced by the broker to complete the reconciliation process.
        commitResult = commitRequestManager.maybeAutoCommitSyncBeforeRevocation(getDeadlineMsForTimeout(rebalanceTimeoutMs));
                // member out of the group after the rebalance timeout expires, leading to a

// After
     * Reconciliation commit timeout. To be used as time limit for the commit request issued
    private final int commitTimeoutDuringReconciliation;
                                 int commitTimeoutDuringReconciliation,
            commitTimeoutDuringReconciliation,
                          int commitTimeoutDuringReconciliation,
        this.commitTimeoutDuringReconciliation = commitTimeoutDuringReconciliation;
        // the current reconciliation is in process. Note this is using the reconciliation commit timeout
        // as it is the limit enforced by the broker to complete the reconciliation process.
        commitResult = commitRequestManager.maybeAutoCommitSyncBeforeRevocation(getDeadlineMsForTimeout(commitTimeoutDuringReconciliation));
                // member out of the group after the reconciliation commit timeout expires, leading to a
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16899 membershipmanagerimpl: rebalancetimeoutms variable name changed to committimeoutduringreconciliation (#16334.

**Test**
No test added.
