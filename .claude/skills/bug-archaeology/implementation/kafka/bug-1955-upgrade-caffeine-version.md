**Commit**: b42efc7dc2 · **Ticket**: KAFKA-18049
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
caffeine-2.9.3
    // In this this test we simulate the situation using latches. We perform the following operations:
    // Reduce cache capacity to only store 1 entries
  caffeine: "2.9.3", // 3.x supports JDK 11 and above

// After
caffeine-3.1.1
    // In this test we simulate the situation using latches. We perform the following operations:
    // Reduce cache capacity to only store 1 entry
  // Version >=3.1.2 includes an improvement to prevent hash DOS attacks,
  // but currently, tests are failing in >=3.1.2. Therefore, we are temporarily using version 3.1.1.
  // The failing tests should be fixed under KAFKA-18089, allowing us to upgrade to >=3.1.2.
  caffeine: "3.1.1",
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18049: upgrade the caffeine version to 3.1.1 (#17879).

**Test**
Test modified in RemoteIndexCacheTest.scala covering the corrected behaviour.
