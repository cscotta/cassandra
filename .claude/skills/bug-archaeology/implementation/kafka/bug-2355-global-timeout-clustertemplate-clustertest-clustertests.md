**Commit**: 79753594ca · **Ticket**: KAFKA-16813
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: TaiJuWu <tjwu1217@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.junit.jupiter.api.Timeout
@Timeout(120)
import org.junit.jupiter.api.Timeout
@Timeout(120)
import org.junit.jupiter.api.Timeout
@Timeout(120)

// After
import org.junit.jupiter.api.Timeout;
@Timeout(60)
import org.junit.jupiter.api.Timeout;
@Timeout(60)
import org.junit.jupiter.api.Timeout;
@Timeout(60)
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16813 add global timeout (60s) for `@clustertemplate`, `@clustertest` and `@clustertests` (#16957).

**Test**
Test modified in ClusterTemplate.java covering the corrected behaviour.
