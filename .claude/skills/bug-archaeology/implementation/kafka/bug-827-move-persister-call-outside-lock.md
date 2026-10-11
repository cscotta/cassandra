**Commit**: ddab943b0b · **Ticket**: KAFKA-18265
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Minor PR to move persister call outside of the lock. The lock is not
required while making the persister call. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>, Abhinav Dixit <adixit@confluent.io>

**Before/After**
```java
// Before

                if (!stateBatches.isEmpty()) {
                    writeShareGroupState(stateBatches).whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.debug("Failed to write the share group state on acquisition lock timeout for share partition: {}-{} memberId: {}",
                                groupId, topicIdPartition, memberId, exception);
                        }
                        // Even if write share group state RPC call fails, we will still go ahead with the state transition.
                        // Update the cached state and start and end offsets after releasing the acquisition lock on timeout.
                        maybeUpdateCachedStateAndOffsets();
                    });
                }

// After
            if (!stateBatches.isEmpty()) {
                writeShareGroupState(stateBatches).whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.debug("Failed to write the share group state on acquisition lock timeout for share partition: {}-{} memberId: {}",
                            groupId, topicIdPartition, memberId, exception);
                    }
                    // Even if write share group state RPC call fails, we will still go ahead with the state transition.
                    // Update the cached state and start and end offsets after releasing the acquisition lock on timeout.
                    maybeUpdateCachedStateAndOffsets();
                });
            }

```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18265: move persister call outside of the lock (3/n) (#20316).

**Test**
No test added.
