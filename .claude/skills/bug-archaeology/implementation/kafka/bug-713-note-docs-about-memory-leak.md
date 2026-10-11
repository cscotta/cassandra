**Commit**: f5a87b3703 · **Ticket**: KAFKA-19748
**Subsystem**: docs/streams
**Category**: Resource leak

**Root cause**
Added a note regarding the memory leak bug in the documentation. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
    <p><b>Note:</b> Kafka Streams 4.1.0 contains a critical memory leak bug (<a href="https://issues.apache.org/jira/browse/KAFKA-19748">KAFKA-19748</a>) that affects users of range scans and certain DSL operators (session windows, sliding windows, stream-stream joins, foreign-key joins). Users running Kafka Streams should consider upgrading directly to 4.1.1 when available.</p>

<p><b>Note:</b> Kafka Streams 4.1.0 contains a critical memory leak bug (<a href="https://issues.apache.org/jira/browse/KAFKA-19748">KAFKA-19748</a>) that affects users of range scans and certain DSL operators (session windows, sliding windows, stream-stream joins, foreign-key joins). Users running Kafka Streams should consider upgrading directly to 4.1.1 when available.</p>

```

**How to spot**
When reviewing docs/streams code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19748: add a note in docs about memory leak in kafka streams 4.1.0  (#20639).

**Test**
No test added.
