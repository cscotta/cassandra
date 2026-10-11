**Commit**: 4f2114a49e · **Ticket**: KAFKA-19645
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Add a lower bound to num.replica.fetchers. Reviewers: PoAn Yang <payang@apache.org>, TaiJuWu <tjwu1217@gmail.com>,
 Ken Huang <s7133700@gmail.com>, jimmy <wangzhiwang611@gmail.com>,
 Jhen-Yung Hsu <jhenyunghsu@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
        case ReplicationConfigs.NUM_REPLICA_FETCHERS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number")
            .define(NUM_REPLICA_FETCHERS_CONFIG, INT, NUM_REPLICA_FETCHERS_DEFAULT, HIGH, NUM_REPLICA_FETCHERS_DOC)

// After
        case ReplicationConfigs.NUM_REPLICA_FETCHERS_CONFIG => assertPropertyInvalid(baseProperties, name, "not_a_number", "0", "-1")
    <li>
        The <code>num.replica.fetchers</code> config has a new lower bound of 1.
    </li>
            .define(NUM_REPLICA_FETCHERS_CONFIG, INT, NUM_REPLICA_FETCHERS_DEFAULT, atLeast(1), HIGH, NUM_REPLICA_FETCHERS_DOC)
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19645 add a lower bound to num.replica.fetchers (#20414).

**Test**
Test modified in KafkaConfigTest.scala covering the corrected behaviour.
