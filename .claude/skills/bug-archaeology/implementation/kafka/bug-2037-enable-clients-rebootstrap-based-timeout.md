**Commit**: 52d2fa5c8b · **Ticket**: KAFKA-17885
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Implementation of https://cwiki.apache.org/confluence/display/KAFKA/KIP-1102%3A+Enable+clients+to+rebootstrap+based+on+timeout+or+error+code
- Introduces rebootstrap trigger interval config metadata.recovery.rebootstrap.trigger.ms, set to 5 minutes by default
- Makes rebootstrap the default for metadata.recovery.strategy
- Adds new error code REBOOTSTRAP_REQUIRED, introduces top-level error code in metadata response. On this error, clients rebootstrap. - Configs apply to producers, consumers, share consumers, admin clients, Connect and KStreams clients. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>,  Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
              files="(ConsumerCoordinator|KafkaConsumer|RequestResponse|Fetcher|FetchRequestManager|KafkaAdminClient|Message|KafkaProducer)Test.java"/>
            "for the client.";
    public static final String DEFAULT_METADATA_RECOVERY_STRATEGY = MetadataRecoveryStrategy.NONE.name;

// After
              files="(ConsumerCoordinator|KafkaConsumer|RequestResponse|Fetcher|FetchRequestManager|KafkaAdminClient|Message|KafkaProducer|NetworkClient)Test.java"/>
                    config.getLong(CommonClientConfigs.METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_CONFIG),
            "for the client. Rebootstrap is also triggered if connection cannot be established to any of the brokers for " +
            "<code>metadata.recovery.rebootstrap.trigger.ms</code> milliseconds or if server requests rebootstrap.";
    public static final String DEFAULT_METADATA_RECOVERY_STRATEGY = MetadataRecoveryStrategy.REBOOTSTRAP.name;

    public static final String METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_CONFIG = "metadata.recovery.rebootstrap.trigger.ms";
    public static final String METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_DOC = "If a client configured to rebootstrap using " +
            "<code>metadata.recovery.strategy=rebootstrap</code> is unable to obtain metadata from any of the brokers in the last known " +
            "metadata for this interval, client repeats the bootstrap process using <code>bootstrap.servers</code> configuration.";
    public static final long DEFAULT_METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS = 300 * 1000;
    /**
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17885: enable clients to rebootstrap based on timeout or error code (kip-1102) (#17720).

**Test**
Test modified in MockClient.java covering the corrected behaviour.
