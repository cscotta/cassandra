**Commit**: 8f98b97454 · **Ticket**: KAFKA-19492
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Currently, troubleshooting OFFSET_OUT_OF_RANGE errors during record
deletion is challenging because deleteRecordsOnLocalLog suppresses the
detailed messages carried by OffsetOutOfRangeException. To enhance the diagnostic capabilities of the Kafka Admin Client, we
should log these specific exception details at the debug level. This
ensures that the original context of the failure is preserved for
developers and operators. Reviewers: Gaurav Narula <gaurav_narula2@apple.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
                   _: OffsetOutOfRangeException |

// After
          case e: OffsetOutOfRangeException =>
            debug("Error processing delete records operation on partition %s".format(topicPartition), e)
            (topicPartition, LogDeleteRecordsResult(-1L, -1L, Some(e)))
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19492 log detailed message for offsetoutofrangeexception in deleterecordsonlocallog (#20141).

**Test**
No test added.
