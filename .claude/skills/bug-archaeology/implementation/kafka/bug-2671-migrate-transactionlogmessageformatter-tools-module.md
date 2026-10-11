**Commit**: ee68f3215f · **Ticket**: KAFKA-16666
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  configurations {
    generator
  }


    generator project(':generator')
  task processMessages(type:JavaExec) {
    mainClass = "org.apache.kafka.message.MessageGenerator"
    classpath = configurations.generator
    args = [ "-p", "kafka.internals.generated",
             "-o", "src/generated/java/kafka/internals/generated",
             "-i", "src/main/resources/common/message",

// After
  configurations {
    generator
  }

    implementation libs.jacksonDatabind
    generator project(':generator')

  task processMessages(type:JavaExec) {
    mainClass = "org.apache.kafka.message.MessageGenerator"
    classpath = configurations.generator
    args = [ "-p", "org.apache.kafka.coordinator.transaction.generated",
             "-o", "src/generated/java/org/apache/kafka/coordinator/transaction/generated",
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16666 migrate `transactionlogmessageformatter` to tools module (#16019).

**Test**
Test modified in TransactionLogTest.scala covering the corrected behaviour.
