**Commit**: b436499557 · **Ticket**: KAFKA-17524
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Adds a 30s default timeout to StreamThreadTest tests. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.Timeout;
@Timeout(300)
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17524: add timeout to streamthreadtest (#17180).

**Test**
Test modified in StreamThreadTest.java covering the corrected behaviour.
