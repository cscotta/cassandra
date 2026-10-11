**Commit**: a1372ced69 · **Ticket**: KAFKA-15583
**Subsystem**: docs/design.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Matthias J. Sax <matthias@confluent.io>, Dave Troiano <dtroiano@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    have the option of either waiting for the message to be committed or not, depending on their preference for tradeoff between latency and durability. This preference is controlled by the acks setting that the
    Note that topics have a setting for the "minimum number" of in-sync replicas that is checked when the producer requests acknowledgment that a message
    has been written to the full set of in-sync replicas. If a less stringent acknowledgement is requested by the producer, then the message can be committed, and consumed,
    even if the number of in-sync replicas is lower than the minimum (e.g. it can be as low as just the leader).

// After
    have the option of either waiting for the message to be committed or not, depending on their preference for tradeoff between latency and durability. This preference is controlled by the <code>acks</code> setting that the
    Note that topics have a setting for the minimum number of in-sync replicas (<code>min.insync.replicas</code>) that is checked when the producer requests acknowledgment that a message
    has been written to the full set of in-sync replicas. If a less stringent acknowledgment is requested by the producer, then the message is committed asynchronously across the set of in-sync replicas if
    <code>acks=0</code>, or synchronously only on the leader if <code>acks=1</code>.
    Regardless of the <code>acks</code> setting, the messages will not be visible to the consumers until all the following conditions are met:
    <ol>
        <li>The messages are replicated to all the in-sync replicas.</li>
        <li>The number of the in-sync replicas is no less than the <code>min.insync.replicas</code> setting.</li>
    </ol>
```

**How to spot**
When reviewing docs/design.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15583 doc update for the "strict min isr" rule (#18880).

**Test**
No test added.
