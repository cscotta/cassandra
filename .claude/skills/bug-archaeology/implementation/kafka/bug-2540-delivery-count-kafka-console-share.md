**Commit**: 34475070e1 · **Ticket**: KAFKA-17368
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
Now that ConsumerRecord.deliveryCount() exists, enhance kafka-console-share-consumer.sh to exploit it. Added support to the DefaultMessageFormatter and the option print.delivery to the usage message for kafka-console-share-consumer.sh. Note that it was not added to kafka-console-consumer.sh even though the option would be recognised - this is because delivery with a consumer group does not count deliveries, and the result would include Delivery:NOT_PRESENT for all records if it was enabled - not really that useful with a consumer group. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                writeSeparator(output, printOffset || printPartition || printHeaders || printKey || printValue);
                writeSeparator(output, printOffset || printHeaders || printKey || printValue);
                headers, Optional.empty());
        assertEquals("CreateTime:123\tPartition:0\tOffset:123\th1:v1,h2:v2\tkey\tvalue\n", out.toString());

// After
                                " print.delivery=true|false\n" +
    private boolean printDelivery = false;
        if (configs.containsKey("print.delivery")) {
            printDelivery = getBoolProperty(configs, "print.delivery");
        }
                writeSeparator(output, printPartition || printOffset || printDelivery || printHeaders || printKey || printValue);
                writeSeparator(output, printOffset || printDelivery || printHeaders || printKey || printValue);
                writeSeparator(output, printDelivery || printHeaders || printKey || printValue);
            }

            if (printDelivery) {
                output.print("Delivery:");
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17368 add delivery count to kafka-console-share-consumer.sh (#16925).

**Test**
Test modified in DefaultMessageFormatterTest.java covering the corrected behaviour.
