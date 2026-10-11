**Commit**: dd755b7ea9 · **Ticket**: KAFKA-8206
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This commit implements KIP-899: Allow producer and consumer clients to rebootstrap. It introduces the new setting `metadata.recovery.strategy`, applicable to all the types of clients. Reviewers: Greg Harris <gharris1727@gmail.com>, Rajini Sivaram <rajinisivaram@googlemail.com>

**Before/After**
```java
// Before
                    clientTelemetrySender);
    Node leastLoadedNode(long now);

// After
                    clientTelemetrySender,
                    MetadataRecoveryStrategy.forName(config.getString(CommonClientConfigs.METADATA_RECOVERY_STRATEGY_CONFIG))
            );
    public static final String METADATA_RECOVERY_STRATEGY_CONFIG = "metadata.recovery.strategy";
    public static final String METADATA_RECOVERY_STRATEGY_DOC = "Controls how the client recovers when none of the brokers known to it is available. " +
            "If set to <code>none</code>, the client fails. If set to <code>rebootstrap</code>, " +
            "the client repeats the bootstrap process using <code>bootstrap.servers</code>. " +
            "Rebootstrapping is useful when a client communicates with brokers so infrequently " +
            "that the set of brokers may change entirely before the client refreshes metadata. " +
            "Metadata recovery is triggered when all last-known brokers appear unavailable simultaneously. " +
            "Brokers appear unavailable when disconnected and no current retry attempt is in-progress. " +
            "Consider increasing <code>reconnect.backoff.ms</code> and <code>reconnect.backoff.max.ms</code> and " +
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-8206: kip-899: allow client to rebootstrap  (#13277).

**Test**
Test modified in MockClient.java covering the corrected behaviour.
