**Commit**: 4e76cc19cd · **Ticket**: KAFKA-8416
**Subsystem**: docs/streams
**Category**: Silent skip on non-match

**Root cause**
The existing docs mentioned both steps in a single dense sentence,
making it easy to miss that Properties must also be passed to
StreamsBuilder.build(). Added a numbered list and code example clearly showing both steps,  with
an explicit warning that calling build() without Properties  silently
skips optimizations even when the config is set. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
Note that you need to do two things to enable optimizations. In addition to setting this config to `StreamsConfig.OPTIMIZE`, you'll need to pass in your configuration properties when building your topology by using the overloaded `StreamsBuilder.build(Properties)` method. For example `KafkaStreams myStream = new KafkaStreams(streamsBuilder.build(properties), properties)`.

    /** {@code topology.optimization} */

// After
**Important:** Enabling optimizations requires two steps. Both are necessary — setting the config alone is not enough:

1. Set `topology.optimization` to `StreamsConfig.OPTIMIZE` (or a comma-separated list of specific optimizations) in your `Properties` object.
2. Pass the same `Properties` object to the overloaded `StreamsBuilder.build(Properties)` method when building your topology.

For example:

```java
Properties properties = new Properties();
properties.put(StreamsConfig.TOPOLOGY_OPTIMIZATION_CONFIG, StreamsConfig.OPTIMIZE);

// Step 2: pass properties to build() — this is required for optimizations to take effect
```

**How to spot**
When reviewing docs/streams code, look for silent skip on non-match patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-8416: clarify two-step requirement for enabling streams topology optimizations (#21556).

**Test**
No test added.
