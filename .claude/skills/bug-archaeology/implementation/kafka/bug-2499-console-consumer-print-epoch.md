**Commit**: cb19a557e9 · **Ticket**: KAFKA-17373
**Subsystem**: tools/consumer/DefaultMessageFormatter
**Category**: Missing null/bounds check

**Root cause**
`DefaultMessageFormatter` supported `print.timestamp`, `print.key`, `print.offset`, `print.partition`, and `print.delivery` but had no `print.epoch` option to display the leader epoch from a `ConsumerRecord`. Users debugging tiered storage or replication issues who wanted to correlate records with leader epochs had no way to include that field in console output without writing a custom formatter. The fix adds `printEpoch` as a new configurable flag, wires it into the output chain between `printDelivery` and `printHeaders`, and documents it in both `ConsoleConsumerOptions` and `ConsoleShareConsumerOptions`.

**Before/After**
```java
// Before: no print.epoch support
if (printDelivery) {
    output.print("Delivery:");
    output.print(consumerRecord.deliveryCount()...);
    writeSeparator(output, printHeaders || printKey || printValue);
}

// After
if (printEpoch) {
    output.print("Epoch:");
    output.print(consumerRecord.leaderEpoch().map(Object::toString).orElse("NOT_PRESENT"));
    writeSeparator(output, printHeaders || printKey || printValue);
}
```

**How to spot**
When adding a new consumer record field to `ConsumerRecord`, check whether `DefaultMessageFormatter` needs a corresponding `print.<field>` option. A missing option forces users to write custom formatters for debugging rather than using the built-in tool.

**Test**
`DefaultMessageFormatterTest` updated to verify `print.epoch=true` emits the leader epoch field in the correct column position.
