**Commit**: 31f79055ce · **Ticket**: KAFKA-17306
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
This patch fixes a few buts in the replay logic of the consumer group records:
* The first issue is that the logic assumed that the group or the member exists when tombstones are replayed. Obviously, this is incorrect after a restart. The group or the member may not me there anymore if the __consumer_offsets partitions only contains tombstones for the group or the member. The patch fixes this by considering tombstones as no-ops if the entity does not exist.

**Before/After**
```java
// Before
                  try {
                    coordinator.replay(
                      record.offset(),
                      batch.producerId,
                      batch.producerEpoch,
                      deserializer.deserialize(record.key, record.value)
                    )
                  } catch {
                    case ex: UnknownRecordTypeException =>
                      warn(s"Unknown record type ${ex.unknownType} while loading offsets and group metadata " +
                        s"from $tp. Ignoring it. It could be a left over from an aborted upgrade.")

// After
                    if (isTraceEnabled) {
                      trace(s"Replaying end transaction marker from $tp at offset ${record.offset} to commit transaction " +
                        s"with producer id ${batch.producerId} and producer epoch ${batch.producerEpoch}.")
                    }
                    if (isTraceEnabled) {
                      trace(s"Replaying end transaction marker from $tp at offset ${record.offset} to abort transaction " +
                        s"with producer id ${batch.producerId} and producer epoch ${batch.producerEpoch}.")
                    }

                  val coordinatorRecordOpt = {
                    try {
                      Some(deserializer.deserialize(record.key, record.value))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17306; soften the validation when replaying tombstones (#16898).

**Test**
Test modified in GroupCoordinatorIntegrationTest.scala covering the corrected behaviour.
