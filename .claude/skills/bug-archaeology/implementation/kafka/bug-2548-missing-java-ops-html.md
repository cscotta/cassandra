**Commit**: 4b41071165 · **Ticket**: KAFKA-17086
**Subsystem**: docs/ops.html
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  Java 8, Java 11, and Java 17 are supported.

// After
  Java 8, Java 11, Java 17, and Java 21 are supported.
```

**How to spot**
When reviewing docs/ops.html code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17086 add missing java 21 to ops.html (#16911).

**Test**
No test added.
