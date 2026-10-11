**Commit**: 089cbefac9 · **Ticket**: KAFKA-16830
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            case "kafka.tools.DefaultMessageFormatter":
                System.err.println("WARNING: kafka.tools.DefaultMessageFormatter is deprecated and will be removed in the next major release. " +
                        "Please use org.apache.kafka.tools.consumer.DefaultMessageFormatter instead");
                return DefaultMessageFormatter.class.getName();
            case "kafka.tools.LoggingMessageFormatter":
                System.err.println("WARNING: kafka.tools.LoggingMessageFormatter is deprecated and will be removed in the next major release. " +
                        "Please use org.apache.kafka.tools.consumer.LoggingMessageFormatter instead");
                return LoggingMessageFormatter.class.getName();
            case "kafka.tools.NoOpMessageFormatter":
                System.err.println("WARNING: kafka.tools.NoOpMessageFormatter is deprecated and will be removed in the next major release. " +
                        "Please use org.apache.kafka.tools.consumer.NoOpMessageFormatter instead");
                return NoOpMessageFormatter.class.getName();

// After
            <li>
                The <code>kafka.tools.DefaultMessageFormatter</code> class has been removed. Please use the <code>org.apache.kafka.tools.consumer.DefaultMessageFormatter</code> class instead.
            </li>
            <li>
                The <code>kafka.tools.LoggingMessageFormatter</code> class has been removed. Please use the <code>org.apache.kafka.tools.consumer.LoggingMessageFormatter</code> class instead.
            </li>
            <li>
                The <code>kafka.tools.NoOpMessageFormatter</code> class has been removed. Please use the <code>org.apache.kafka.tools.consumer.NoOpMessageFormatter</code> class instead.
            </li>
    public void testParseFormatter() throws Exception {
        String[] defaultMessageFormatter = generateArgsForFormatter("org.apache.kafka.tools.consumer.DefaultMessageFormatter");
        assertInstanceOf(DefaultMessageFormatter.class, new ConsoleConsumerOptions(defaultMessageFormatter).formatter());
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16830 remove the scala version formatters support (#17127).

**Test**
Test modified in ConsoleConsumerOptionsTest.java covering the corrected behaviour.
