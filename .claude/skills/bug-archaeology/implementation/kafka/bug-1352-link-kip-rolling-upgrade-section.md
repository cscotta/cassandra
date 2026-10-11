**Commit**: be9c4a4fab · **Ticket**: KAFKA-18422
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Add client upgrade documentation, for more information, please refer to https://cwiki.apache.org/confluence/x/y4kgF

Reviewers: TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
    <li>For the Kafka client upgrade path, note that many deprecated APIs were removed in Kafka 4.0. Additionally, upgrading directly to 4.x from certain versions is not feasible.
        For more information, please refer to <a href="https://cwiki.apache.org/confluence/x/y4kgF">KIP-1124</a>.
    </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18422 add link of kip-1124 to "rolling upgrade" section  (#19097).

**Test**
No test added.
