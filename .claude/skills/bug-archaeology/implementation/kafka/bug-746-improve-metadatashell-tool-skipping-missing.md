**Commit**: 07b786e5bf · **Ticket**: KAFKA-19681
**Subsystem**: metadata/src
**Category**: Logic error in condition

**Root cause**
The current `metadata-shell` find command throws an exception due to
child node `zkMigrationState`. This interrupts the output and makes the CLI less usable. Additionally, `zkMigrationState` is no longer used in Kafka 4.x, but it
is still defined under image/features, which causes misleading error
messages. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * The name of the zk migration state child node.
     */
    public static final String ZK_MIGRATION_STATE = "zkMigrationState";

        childNames.add(ZK_MIGRATION_STATE);

// After
// (no additions)
```

**How to spot**
When reviewing metadata/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19681 improve metadatashell tool by skipping missing children and removing zkmigrationstate (#20504).

**Test**
No test added.
