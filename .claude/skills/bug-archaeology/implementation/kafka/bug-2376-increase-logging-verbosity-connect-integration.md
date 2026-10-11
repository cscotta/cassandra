**Commit**: 70f7418660 · **Ticket**: KAFKA-17493
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
This patch increases the verbosity of the logging in Connect's integration tests. This is to better understand the causes of the flaky tests described in KAFKA-17493. Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
// (no removals)

// After

# Troubleshooting KAFKA-17493.
log4j.logger.org.apache.kafka.consumer=DEBUG
log4j.logger.org.apache.kafka.coordinator.group=DEBUG
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: increase logging verbosity in connect integration tests (#17189).

**Test**
Test modified in log4j.properties covering the corrected behaviour.
