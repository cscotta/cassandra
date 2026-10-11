**Commit**: e136d7611c · **Ticket**: KAFKA-17656
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            log.trace("Try to elect last known leader for " + topicId + "-" + partitionId +
                " but elrEnabled=" + eligibleLeaderReplicasEnabled + ", useLastKnownLeaderInBalancedRecovery=" +
                useLastKnownLeaderInBalancedRecovery);
            log.trace("Try to elect last known leader for " + topicId + "-" + partitionId +
                " but ELR/ISR is not empty. ISR=" + targetIsr + ", ELR=" + targetElr);
            log.trace("Try to elect last known leader for " + topicId + "-" + partitionId +
                " but lastKnownElr does not only have 1 member. lastKnownElr=" +
                Arrays.toString(partition.lastKnownElr));
            log.trace("Try to elect last known leader for " + topicId + "-" + partitionId +
                " but last known leader is not alive. last known leader=" + partition.lastKnownElr[0]);
                log.debug("A partition will have an empty ISR. " + this);

// After
            log.trace("Try to elect last known leader for {}-{} but elrEnabled={}, useLastKnownLeaderInBalancedRecovery={}",
                    topicId, partitionId, eligibleLeaderReplicasEnabled, useLastKnownLeaderInBalancedRecovery);
            log.trace("Try to elect last known leader for {}-{} but ELR/ISR is not empty. ISR={}, ELR={}",
                    topicId, partitionId, targetIsr, targetElr);
            log.trace("Try to elect last known leader for {}-{} but lastKnownElr does not only have 1 member. lastKnownElr={}",
                    topicId, partitionId, Arrays.toString(partition.lastKnownElr));
            log.trace("Try to elect last known leader for {}-{} but last known leader is not alive. last known leader={}",
                    topicId, partitionId, partition.lastKnownElr[0]);
                log.debug("A partition will have an empty ISR. {}", this);
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17656 replace string concatenation with parameterized logging for partitionchangebuilder (#17334).

**Test**
No test added.
