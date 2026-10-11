**Commit**: 22ed71a2c2 · **Ticket**: KAFKA-17602
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.ConfigUtils;
    public static Predicate<String> compilePredicate(Map<String, ?> originalConfig) {
        Map<String, ?> configs = ConfigUtils.translateDeprecatedConfigs(
            originalConfig, new String[][]{{INCLUDE_CONFIG},
                                           {EXCLUDE_CONFIG}});

// After
    public static Predicate<String> compilePredicate(Map<String, ?> configs) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17602 remove `configutils.translatedeprecatedconfigs` from jmxreporter (#17267).

**Test**
No test added.
