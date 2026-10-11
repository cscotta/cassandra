**Commit**: e76e1da5ea · **Ticket**: KAFKA-16935
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <gharris1727@gmail.com>

**Before/After**
```java
// Before
        primary.assertions().assertAtLeastNumWorkersAreUp(NUM_WORKERS,
                "Workers of " + PRIMARY_CLUSTER_ALIAS + "-connect-cluster did not start in time.");
        backup.assertions().assertAtLeastNumWorkersAreUp(NUM_WORKERS,
            "Workers of " + BACKUP_CLUSTER_ALIAS + "-connect-cluster did not start in time.");

        connect.assertions().assertAtLeastNumWorkersAreUp(
                NUM_WORKERS,
                "Initial group of workers did not start in time"
        );
        connect.assertions().assertAtLeastNumWorkersAreUp(NUM_WORKERS,
                "Initial group of workers did not start in time.");


// After
// (no additions)
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16935: automatically wait for cluster startup in embedded connect integration tests (#16288).

**Test**
Test modified in MirrorConnectorsIntegrationBaseTest.java covering the corrected behaviour.
