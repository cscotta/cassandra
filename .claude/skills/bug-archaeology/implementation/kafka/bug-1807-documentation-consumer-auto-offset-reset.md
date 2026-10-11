**Commit**: 4aee33d6ab · **Ticket**: KAFKA-18259
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
            "<ul><li>earliest: automatically reset the offset to the earliest offset" +
            "<li>by_duration:<duration>: automatically reset the offset to a configured <duration> from the current timestamp. <duration> must be specified in ISO8601 format (PnDTnHnMn.nS). " +

// After
            "<ul><li>earliest: automatically reset the offset to the earliest offset</li>" +
            "<li>by_duration:&lt;duration&gt;: automatically reset the offset to a configured &lt;duration&gt; from the current timestamp. &lt;duration&gt; must be specified in ISO8601 format (PnDTnHnMn.nS). " +
import java.util.stream.Collectors;

        @Override
        public String toString() {
            String values = Arrays.stream(StrategyType.values())
                .map(strategyType -> {
                    if (strategyType == StrategyType.BY_DURATION) {
                        return "by_duration:PnDTnHnMn.nS";
                    }
                    return strategyType.toString();
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18259: documentation for consumer auto.offset.reset contains invalid html (#18210).

**Test**
No test added.
