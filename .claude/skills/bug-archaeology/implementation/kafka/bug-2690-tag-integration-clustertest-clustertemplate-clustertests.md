**Commit**: 0696fbc732 · **Ticket**: KAFKA-17172
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import org.junit.jupiter.api.Tag;
@Tag("integration")
import org.junit.jupiter.api.Tag;
@Tag("integration")
import org.junit.jupiter.api.Tag;
@Tag("integration")
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17172 add `@tag("integration")` to clustertest, clustertemplate, and clustertests (#16640).

**Test**
Test modified in ClusterTemplate.java covering the corrected behaviour.
