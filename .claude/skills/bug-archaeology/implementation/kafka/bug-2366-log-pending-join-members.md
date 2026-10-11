**Commit**: 74bebf6e3d · **Ticket**: KAFKA-17493
**Subsystem**: group-coordinator/src
**Category**: Logic error in condition

**Root cause**
I am still chasing KAFKA-17493. I was able to narrow it down to an issue with the pending join members. This patch logs them in order to help me troubleshooting it further. I will revert this change when the issue is root caused.

**Before/After**
```java
// Before
                group.groupId(), group.numMembers(), group.numAwaitingJoinResponse(), group.numPendingJoinMembers());

// After
                group.groupId(), group.numMembers(), group.numAwaitingJoinResponse(), String.join(",", group.pendingJoinMembers()));
    /**
     * @return The set of pending join members.
     */
    public Set<String> pendingJoinMembers() {
        return Collections.unmodifiableSet(pendingJoinMembers);
    }

```

**How to spot**
When reviewing group-coordinator/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: log pending join members (#17219).

**Test**
No test added.
