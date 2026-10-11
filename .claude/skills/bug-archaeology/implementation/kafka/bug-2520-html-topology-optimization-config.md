**Commit**: 0300025097 · **Ticket**: KAFKA-14209
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The HTML rendering broke via https://issues.apache.org/jira/browse/KAFKA-14209 in 3.4 release. The currently shown value is some garbage org.apache.kafka.streams.StreamsConfig$$Lambda$20/0x0000000800c0cf18@b1bc7ed

cf https://kafka.apache.org/documentation/#streamsconfigs_topology.optimization

Verified the fix via running StreamsConfig#main() locally. Reviewers: Bill Bejeck <bill@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                    (name, value) -> verifyTopologyOptimizationConfigs((String) value),

// After
                    new ConfigDef.Validator() {
                        @Override
                        public void ensureValid(final String name, final Object value) {
                            verifyTopologyOptimizationConfigs((String) value);
                        }

                        @Override
                        public String toString() {
                            return TOPOLOGY_OPTIMIZATION_CONFIGS.toString();
                        }
                    },
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: fix html for topology.optimization config (#16953).

**Test**
No test added.
