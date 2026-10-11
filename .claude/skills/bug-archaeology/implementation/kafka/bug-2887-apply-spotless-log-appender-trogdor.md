**Commit**: 46eb0814f6 · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                               ':examples',
                               ':generator',
                               ':jmh-benchmarks',
                               ':log4j-appender',
                               ':shell',
                               ':streams:upgrade-system-tests-37',
                               ':trogdor']
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;

// After
                               ':streams:upgrade-system-tests-37']

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertTrue;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to log4j-appender, trogdor, jmh-benchmarks, examples, shell and generator (#16296).

**Test**
Test modified in CodeBufferTest.java covering the corrected behaviour.
