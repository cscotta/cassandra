**Commit**: b5cceb43e5 · **Ticket**: KAFKA-19205
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
In the return results of the methods beginningOffsets and endOffset, if
timeout == 0, then an empty Map should be returned uniformly instead of
in the form of <TopicPartition, null>

Reviewers: Ken Huang <s7133700@gmail.com>, PoAn Yang
 <payang@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>, Lianet
 Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
     * @return The earliest available offsets for the given partitions
     * @return The end offsets for the given partitions.
                return listOffsetsEvent.emptyResults();
        // The result should be {tp=null}
        assertTrue(result.containsKey(tp));
        assertNull(result.get(tp));

// After
     * @return The earliest available offsets for the given partitions, and it will return empty map if zero timeout is provided
     * @return The end offsets for the given partitions, and it will return empty map if zero timeout is provided
                // It is used to align with classic consumer.
                // When the "timeout == 0", the classic consumer will return an empty map.
                // Therefore, the AsyncKafkaConsumer needs to be consistent with it.
                return new HashMap<>();
        assertNotNull(result);
        assertEquals(0, result.size());
import java.lang.{Long => JLong}

  @ParameterizedTest(name = TestInfoUtils.TestWithParameterizedGroupProtocolNames)
  @MethodSource(Array("getTestGroupProtocolParametersAll"))
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19205: inconsistent result of beginningoffsets/endoffset between classic and async consumer with 0 timeout (#19578.

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
