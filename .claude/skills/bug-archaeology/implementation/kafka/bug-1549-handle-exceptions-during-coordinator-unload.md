**Commit**: 5946f27ac5 · **Ticket**: KAFKA-18484
**Subsystem**: coordinator-common/src
**Category**: Resource leak

**Root cause**
Ensure that unloading a coordinator always succeeds. Previously, we have
guarded against exceptions from DeferredEvent completions. All that
remains is handling exceptions from the onUnloaded() method of the
coordinator state machine. Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
                coordinator.onUnloaded();
                        context.transitionTo(CoordinatorState.CLOSED);
                        coordinators.remove(tp, context);
                        log.info("Finished unloading metadata for {} with epoch {}.", tp, partitionEpoch);

// After
                try {
                    coordinator.onUnloaded();
                } catch (Throwable ex) {
                    log.error("Failed to unload coordinator for {} due to {}.", tp, ex.getMessage(), ex);
                }
                        try {
                            context.transitionTo(CoordinatorState.CLOSED);
                            log.info("Finished unloading metadata for {} with epoch {}.", tp, partitionEpoch);
                        } catch (Throwable ex) {
                            // It's very unlikely that we will ever see an exception here, since we
                            // already make an effort to catch exceptions in the unload method.
                            log.error("Failed to unload metadata for {} with epoch {} due to {}.",
```

**How to spot**
When reviewing coordinator-common/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18484 [2/2]; handle exceptions during coordinator unload (#18667).

**Test**
Test modified in CoordinatorRuntimeTest.java covering the corrected behaviour.
