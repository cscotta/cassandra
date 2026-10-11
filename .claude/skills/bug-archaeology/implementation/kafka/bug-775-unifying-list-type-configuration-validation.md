**Commit**: 0a12eaa80e · **Ticket**: KAFKA-19112
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
We add the three main changes in this PR

- Disallowing null values for most LIST-type configurations makes sense,
since users cannot explicitly set a configuration to null in a
properties file. Therefore, only configurations with a default value of
null should be allowed to accept null. - Disallowing duplicate values is reasonable, as there are currently no
known configurations in Kafka that require specifying the same value
multiple times. Allowing duplicates is both rare in practice and
potentially confusing to users.

**Before/After**
```java
// Before
                                        "",
                                         "",
                                        List.of(),
import java.util.Collections;
            RANGE_ASSIGNOR_NAME,
            ROUNDROBIN_ASSIGNOR_NAME,
            STICKY_ASSIGNOR_NAME,


// After
                                        List.of(),
                                        ConfigDef.ValidList.anyNonDuplicateValues(true, false),
                                         List.of(),
                                         ConfigDef.ValidList.anyNonDuplicateValues(true, false),
                                        ConfigDef.ValidList.anyNonDuplicateValues(true, false),
                                        List.of(),
                                        ConfigDef.ValidList.anyNonDuplicateValues(true, false),
            RANGE_ASSIGNOR_NAME,
            ROUNDROBIN_ASSIGNOR_NAME,
            STICKY_ASSIGNOR_NAME,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19112 unifying list-type configuration validation and default values (#20334).

**Test**
Test modified in GroupRebalanceConfigTest.java covering the corrected behaviour.
