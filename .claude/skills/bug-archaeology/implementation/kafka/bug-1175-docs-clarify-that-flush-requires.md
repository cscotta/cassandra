**Commit**: effbad9e80 · **Ticket**: KAFKA-19151
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Enhanced docs of `flush.ms` to remind users the flush is triggered by
`log.flush.scheduler.interval.ms`. Reviewers: PoAn Yang <payang@apache.org>, Ken Huang
<s7133700@gmail.com>, TengYao Chi <kitingiao@gmail.com>, Chia-Ping Tsai
<chia7712@gmail.com>

**Before/After**
```java
// Before
        "we would fsync after 1000 ms had passed. In general we recommend you not set " +
        "this and use replication for durability and allow the operating system's background " +
        "flush capabilities as it is more efficient.";

// After
        "we would fsync after 1000 ms had passed. Note that this setting depends on the broker-level " +
        "configuration <code>log.flush.scheduler.interval.ms</code>, which controls how frequently the flush check occurs. " +
        "In general we recommend you not set this and use replication for durability and allow the operating system's " +
        "background flush capabilities as it is more efficient.";
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19151 docs: clarify that flush.ms requires log.flush.scheduler.interval.ms config (#19479).

**Test**
No test added.
