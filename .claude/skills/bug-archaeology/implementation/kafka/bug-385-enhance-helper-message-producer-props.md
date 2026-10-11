**Commit**: 047ae5c4c2 · **Ticket**: KAFKA-20281
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
* Add helper message for `--producer-props` and `--command-property`. * Add related test cases. Reviewers: Ken Huang <s7133700@gmail.com>, Nilesh Kumar
 <nileshkumar3@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                        "This option will be removed in a future version. Use --command-property instead.");
                        "These configs take precedence over those passed via --command-config or --producer.config.");

// After
                        "This option will be removed in a future version. Use --command-property instead. Example: --command-property linger.ms=10 batch.size=32768");
                        "These configs take precedence over those passed via --command-config or --producer.config. Example: --command-property linger.ms=10 batch.size=32768");
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

    @ParameterizedTest
    @ValueSource(strings = {"--command-property", "--producer-props"})
    public void testMultipleProducerProperty(String configKey) throws IOException, ArgumentParserException {
        ArgumentParser parser = ProducerPerformance.argParser();
        String[] args = new String[]{
            "--topic", "Hello-Kafka",
            "--num-records", "5",
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20281 enhance helper message for producer-props and command-property (#21688).

**Test**
Test modified in ProducerPerformanceTest.java covering the corrected behaviour.
