**Commit**: 7fb25a2b06 · **Ticket**: KAFKA-16769
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public static final String ADD_SOURCE_ALIAS_TO_METRICS = "add.source.alias.to.metrics";
    private static final String ADD_SOURCE_ALIAS_TO_METRICS_DOC = "Deprecated. Whether to tag metrics with the source cluster alias. "
        + "Metrics have the target, topic and partition tags. When this setting is enabled, it adds the source tag. "
        + "This configuration will be removed in Kafka 4.0 and the default behavior will be to always have the source tag.";
    public static final boolean ADD_SOURCE_ALIAS_TO_METRICS_DEFAULT = false;
    boolean addSourceAliasToMetrics() {
        return getBoolean(ADD_SOURCE_ALIAS_TO_METRICS);
    }

                .define(
                        ADD_SOURCE_ALIAS_TO_METRICS,
                        ConfigDef.Type.BOOLEAN,

// After
        Set<String> partitionTags = new HashSet<>(Arrays.asList("source", "target", "topic", "partition"));
            tags.put("source", source);
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16769 remove add.source.alias.to.metrics configuration (#17323).

**Test**
Test modified in MirrorSourceMetricsTest.java covering the corrected behaviour.
