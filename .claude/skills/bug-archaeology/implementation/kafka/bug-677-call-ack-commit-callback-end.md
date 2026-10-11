**Commit**: 87657fdfc7 · **Ticket**: KAFKA-19827
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The acknowledgement commit callback in the share consumer gets called on
the application thread at the start of the poll, commitSync and
commitAsync methods. Specifically in the peculiar case of using the
callback together with commitSync, the acknowledgement callback for the
committed records is called at the start of the next eligible call, even
though the information is already known at the end of the commitSync's
execution. The results are correct already, but the timing could be
improved in some situations. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>, Shivsundar R
<shr@confluent.io>

**Before/After**
```java
// Before
            TestUtils.waitForCondition(() -> {
                shareConsumer.poll(Duration.ofMillis(500));
                return partitionOffsetsMap.containsKey(tp);
            }, DEFAULT_MAX_WAIT_MS, 100L, () -> "Failed to receive call to callback");
                    return result;

// After
            // The callback should be called before the return of the poll, even when there are no more records.
            ConsumerRecords<byte[], byte[]> records = shareConsumer.poll(Duration.ofMillis(2000));
            assertEquals(0, records.count());
            assertTrue(partitionOffsetsMap.containsKey(tp));

            // We expect no exception as the acknowledgement error code is null.
            assertFalse(partitionExceptionMap.containsKey(tp));
            verifyShareGroupStateTopicRecordsProduced();
        }
    }

    @ClusterTest
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19827: call ack commit callback at end of waiting calls (#20758).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
