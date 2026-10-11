**Commit**: d0ef66bf79 · **Ticket**: KAFKA-19829
**Subsystem**: coordinator-common/src
**Category**: State not cleaned up

**Root cause**
During testing, an artifact of the new rebalance protocol showed up. In
some cases, the first joining member gets all active tasks assigned, and
is slow to revoke the tasks after more member has joined the group. This
affects in particular cases where the first member is slow (possibly
overloaded in the case of cloudlimits benchmarks) and there are a lot of
tasks to be assigned. To help with this situation, we want to introduce a new group-specific
configuration to delay the initial rebalance.

**Before/After**
```java
// Before
// (no removals)

// After
        @Override
        public boolean isScheduled(String key) {
            return tasks.containsKey(key);
        }


    /**
     * Check if an operation with the given key is scheduled.
     *
     * @param key The key.
     * @return True if an operation with the key is scheduled, false otherwise.
     */
```

**How to spot**
When reviewing coordinator-common/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19829: implement group-level initial rebalance delay (#20755).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
