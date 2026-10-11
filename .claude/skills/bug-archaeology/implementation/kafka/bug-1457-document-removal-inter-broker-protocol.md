**Commit**: e2e302b7e6 · **Ticket**: KAFKA-18741
**Subsystem**: docs/zk2kraft.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
        <li>
            <p>
                Remove broker protocol version-related configurations. These configurations were used in ZooKeeper mode to define communication protocol version between brokers.
                In KRaft mode, Kafka uses <code>metadata.version</code> to control the feature level of the cluster, which can be managed using <code>bin/kafka-features.sh</code>.
            </p>
            <ul>
                <li><code>inter.broker.protocol.version</code></li>
            </ul>
        </li>
```

**How to spot**
When reviewing docs/zk2kraft.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18741 document the removal of `inter.broker.protocol.version` (#18818).

**Test**
No test added.
