**Commit**: 46714dbaed · **Ticket**: KAFKA-16933
**Subsystem**: clients/src
**Category**: State not cleaned up

**Root cause**
Fixes for the leave group flow (unsubscribe/close):

Fix to send Heartbeat to leave group on close even if the callbacks fail
fix to ensure that if a member gets fenced while blocked on callbacks (ex. on unsubscribe), it will clear its epoch to not include it in commit requests
fix to avoid race on the subscription state object on unsubscribe, updating it only on the background thread when the callbacks to leave complete (success or failure). Also improving logging in this area. Reviewers: Lucas Brutschy <lbrutschy@confluent.io>, Philip Nee <pnee@confluent.io>

**Before/After**
```java
// Before
        completeQuietly(
            () -> {
                maybeRevokePartitions();
                applicationEventHandler.addAndGet(new LeaveOnCloseEvent(calculateDeadlineMs(timer)));
            },
            "Failed to send leaveGroup heartbeat with a timeout(ms)=" + timer.timeoutMs(), firstException);
                log.info("Unsubscribing all topics or patterns and assigned partitions");
            subscriptions.unsubscribe();
                "max.poll.interval.ms, exceeded approximately by {} ms.", pollTimer.isExpiredBy());
            log.debug("Member {} with epoch {} got fenced but it is already preparing to leave " +
            // Transition to UNSU

// After
        completeQuietly(() -> maybeRevokePartitions(),
            "Failed to execute callback to release assignment", firstException);
        completeQuietly(() -> applicationEventHandler.addAndGet(new LeaveOnCloseEvent(calculateDeadlineMs(timer))),
            "Failed to send leave group heartbeat with a timeout(ms)=" + timer.timeoutMs(), firstException);
            firstException.compareAndSet(null, e);
            log.error(msg, e);
                log.info("Unsubscribing all topics or patterns and assigned partitions {}",
                    subscriptions.assignedPartitions());
        } catch (Exception e) {
            log.error("Unsubscribe failed", e);
            throw e;
                "max.poll.interval.ms, exceeded approximately by {} ms. Member {} will rejoin the group now.",
```

**How to spot**
When reviewing clients/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16933: new consumer unsubscribe close commit fixes (#16272).

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
