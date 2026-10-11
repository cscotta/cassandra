**Commit**: 34afdb9627 · **Ticket**: KAFKA-15045
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
The StreamsConfig class was not parsing the new task assignment
configuration flag properly, which made it impossible to properly
configure a custom task assignor. This PR fixes this and adds a bit of INFO logging to help users diagnose
assignor misconfiguration issues. Reviewers: Anna Sophie Blee-Goldman <ableegoldman@apache.org>

**Before/After**
```java
// Before
// (no removals)

// After
        consumerProps.put(TASK_ASSIGNOR_CLASS_CONFIG, getString(TASK_ASSIGNOR_CLASS_CONFIG));
            log.info("No custom task assignors found, defaulting to internal task assignment with {}", INTERNAL_TASK_ASSIGNOR_CLASS);
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15045: (kip-924 pt. 20) custom task assignment configuration fix (#16245).

**Test**
No test added.
