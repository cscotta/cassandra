**Commit**: 80f31224aa · **Ticket**: KAFKA-10787
**Subsystem**: build.gradle
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
def excludedSpotlessModules = [':clients',
                               ':connect:runtime',
        targetExclude 'src/generated/**/*.java'
import java.util.HashSet;
import java.util.Set;

import java.util.stream.Collectors;
import java.util.OptionalInt;

// After

def excludedSpotlessModules = [':connect:runtime',
        targetExclude('src/generated/**/*.java','src/generated-test/**/*.java')


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;



import java.util.OptionalInt;
```

**How to spot**
When reviewing build.gradle code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-10787 apply spotless to `clients` module  (#16393).

**Test**
Test modified in ApiVersionsTest.java covering the corrected behaviour.
