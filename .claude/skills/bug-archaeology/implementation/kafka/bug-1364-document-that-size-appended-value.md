**Commit**: d460efb9c9 · **Ticket**: KAFKA-18908
**Subsystem**: docs/zk2kraft.html
**Category**: Logic error in condition

**Root cause**
JIRA: KAFKA-18908
`ZooKeeper` mode allows large configuration values to be created through
an append operation. `KRaft` mode restricts configuration values to a
maximum size of `Short.MAX_VALUE`. We should document this behavioral change. Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <h2 class="anchor-heading">Behavioral Change Reference</h2>
    <p>
        This document catalogs the functional and operational differences between ZooKeeper mode and KRaft mode.
    </p>
    <ul>
        <li>
            <strong>Configuration Value Size Limitation</strong>: KRaft mode restricts configuration values to a maximum size of <code>Short.MAX_VALUE</code>,
            which prevents using the append operation to create larger configuration values.
        </li>
    </ul>
```

**How to spot**
When reviewing docs/zk2kraft.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18908 document that the size of appended value can't be larger than short.max_value (#19067).

**Test**
No test added.
