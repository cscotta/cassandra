**Commit**: 6c59e657c0 · **Ticket**: KAFKA-17640
**Subsystem**: docs/ops.html
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  Java 11, Java 17, and Java 21 are supported.

// After
  Java 11, Java 17, Java 21 and Java 23 are supported.
                <li>
                    Java 23 support has been added in Apache Kafka 4.0
                </li>
```

**How to spot**
When reviewing docs/ops.html code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17640 document java 23 support and include release note (#17403).

**Test**
No test added.
