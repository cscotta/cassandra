**Commit**: 9d5dd079fe · **Ticket**: KAFKA-18068
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Document deprecation of PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG
in `upgrade.html`, which was missed in #20317

Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <li>
        The <code>PARTITIONER_ADPATIVE_PARTITIONING_ENABLE_CONFIG</code> in <code>ProducerConfig</code> was deprecated and will be removed in Kafka 5.0. Please use the <code>PARTITIONER_ADAPTIVE_PARTITIONING_ENABLE_CONFIG</code> instead.
    </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18068 document deprecation of partitioner_adpative_partitioning_enable_config (#20322).

**Test**
No test added.
