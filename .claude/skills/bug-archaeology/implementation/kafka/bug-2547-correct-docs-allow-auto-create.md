**Commit**: 5f02ef952e · **Ticket**: KAFKA-17340
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Guozhang Wang <wangguoz@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            " be set to `false` when using brokers older than 0.11.0";

// After
            " be set to `true` when using brokers older than 0.11.0";
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17340 correct the docs of allow.auto.create.topics (#16880).

**Test**
No test added.
