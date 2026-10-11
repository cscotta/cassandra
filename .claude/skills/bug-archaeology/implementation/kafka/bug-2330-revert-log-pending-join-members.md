**Commit**: bd94a739ef · **Ticket**: KAFKA-17571
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
This reverts commit 74bebf6e3dfdc9de6894d656bdbbb28a4c33eda8. Reviewers: David Arthur <mumrah@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                group.groupId(), group.numMembers(), group.numAwaitingJoinResponse(), String.join(",", group.pendingJoinMembers()));
    /**
     * @return The set of pending join members.
     */
    public Set<String> pendingJoinMembers() {
        return Collections.unmodifiableSet(pendingJoinMembers);
    }


// After
                group.groupId(), group.numMembers(), group.numAwaitingJoinResponse(), group.numPendingJoinMembers());
```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17571; revert "minor: log pending join members (#17219)" (#17274).

**Test**
No test added.
