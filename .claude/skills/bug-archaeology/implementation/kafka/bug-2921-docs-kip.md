**Commit**: 6448a59653 · **Ticket**: KAFKA-16911
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            <td colspan="2">A configuration telling Kafka Streams if it should optimize the topology and what optimizations to apply. Acceptable values are: <code>StreamsConfig.NO_OPTIMIZATION</code> (<code>none</code>), <code>StreamsConfig.OPTIMIZE</code> (<code>all</code>) or a comma separated list of specific optimizations: (<code>StreamsConfig.REUSE_KTABLE_SOURCE_TOPICS</code> (<code>reuse.ktable.source.topics</code>), <code>StreamsConfig.MERGE_REPARTITION_TOPICS</code> (<code>merge.repartition.topics</code>)).</td>
            A configuration telling Kafka Streams if it should optimize the topology and what optimizations to apply. Acceptable values are: <code>StreamsConfig.NO_OPTIMIZATION</code> (<code>none</code>), <code>StreamsConfig.OPTIMIZE</code> (<code>all</code>) or a comma separated list of specific optimizations: (<code>StreamsConfig.REUSE_KTABLE_SOURCE_TOPICS</code> (<code>reuse.ktable.source.topics</code>), <code>StreamsConfig.MERGE_REPARTITION_TOPICS</code> (<code>merge.repartition.topics</code>)).

// After
            <td colspan="2">A configuration telling Kafka Streams if it should optimize the topology and what optimizations to apply. Acceptable values are: <code>StreamsConfig.NO_OPTIMIZATION</code> (<code>none</code>), <code>StreamsConfig.OPTIMIZE</code> (<code>all</code>) or a comma separated list of specific optimizations: <code>StreamsConfig.REUSE_KTABLE_SOURCE_TOPICS</code> (<code>reuse.ktable.source.topics</code>), <code>StreamsConfig.MERGE_REPARTITION_TOPICS</code> (<code>merge.repartition.topics</code>),
            <code>StreamsConfig.SINGLE_STORE_SELF_JOIN</code> (<code>single.store.self.join</code>). </td>
            A configuration telling Kafka Streams if it should optimize the topology and what optimizations to apply. Acceptable values are: <code>StreamsConfig.NO_OPTIMIZATION</code> (<code>none</code>), <code>StreamsConfig.OPTIMIZE</code> (<code>all</code>) or a comma separated list of specific optimizations: <code>StreamsConfig.REUSE_KTABLE_SOURCE_TOPICS</code> (<code>reuse.ktable.source.topics</code>), <code>StreamsConfig.MERGE_REPARTITION_TOPICS</code> (<code>merge.repartition.topics</code>), <code>StreamsConfig.SINGLE_STORE_SELF_JOIN</code> (<code>single.store.self.join</code>).
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16911: update docs for kip-862 (#16246).

**Test**
No test added.
