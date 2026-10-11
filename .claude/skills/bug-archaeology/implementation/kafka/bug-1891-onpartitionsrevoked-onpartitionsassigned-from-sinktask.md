**Commit**: 970d8930ac · **Ticket**: KAFKA-18159
**Subsystem**: connect/api
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        this.onPartitionsAssigned(partitions);
    }

    /**
     * @deprecated Use {@link #open(Collection)} for partition initialization.
     */
    @Deprecated
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        this.onPartitionsRevoked(partitions);
    }

    /**

// After
                        <li>The <code>onPartitionsRevoked(Collection&lt;TopicPartition&gt;)</code> and <code>onPartitionsAssigned(Collection&lt;TopicPartition&gt;)</code> methods
                            were removed from <code>SinkTask</code>.
                        </li>
```

**How to spot**
When reviewing connect/api code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18159 remove onpartitionsrevoked and onpartitionsassigned from sinktask (#18049).

**Test**
Test modified in BlockingConnectorTest.java covering the corrected behaviour.
