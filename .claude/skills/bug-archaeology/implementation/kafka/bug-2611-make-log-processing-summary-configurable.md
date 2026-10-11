**Commit**: 5596f9e1d5 · **Ticket**: KAFKA-16584
**Subsystem**: docs/streams
**Category**: Resource leak

**Root cause**
Updates Kafka Streams upgrade-guide for KIP-1049. Reviewers: Bill Bejeck <bill@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    <p>
        Kafka Streams now allows to customize the logging interval of stream-thread runtime summary, via the newly added config <code>log.summary.interval.ms</code>.
        By default, the summary is logged every 2 minutes. More details can be found in
        <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-1049%3A+Add+config+log.summary.interval.ms+to+Kafka+Streams">KIP-1049</a>.
    </p>

```

**How to spot**
When reviewing docs/streams code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16584: make log processing summary configurable or debug--update upgrade-guide (#16709).

**Test**
No test added.
