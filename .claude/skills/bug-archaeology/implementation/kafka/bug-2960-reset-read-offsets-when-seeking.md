**Commit**: 8a882a77a4 · **Ticket**: KAFKA-16105
**Subsystem**: storage/src
**Category**: Resource leak

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>, Luke Chen <showuon@gmail.com>, Kamal Chandraprakash <kamal.chandraprakash@gmail.com>

**Before/After**
```java
// Before
                .stream()
                .filter(utp -> !utp.isAssigned)
                .map(utp -> toRemoteLogPartition(utp.metadataPartition))
                .collect(Collectors.toSet());
}
        consumer = new MockConsumer<>(OffsetResetStrategy.EARLIEST);

// After
                    .stream()
                    .filter(utp -> !utp.isAssigned)
                    .map(utp -> utp.metadataPartition)
                    // When reset to beginning is happening, we also need to reset the last read offset
                    // Otherwise if the next reassignment request for the same metadata partition comes in
                    // before the record of already assigned topic has been read, then the reset will happen again to the last read offset
                    .peek(readOffsetsByMetadataPartition::remove)
                    .map(ConsumerTask::toRemoteLogPartition)
                    .collect(Collectors.toSet());
}
import org.apache.kafka.clients.consumer.ConsumerRecords;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
```

**How to spot**
When reviewing storage/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16105: reset read offsets when seeking to beginning in tbrlmm (#15165).

**Test**
Test modified in ConsumerTaskTest.java covering the corrected behaviour.
