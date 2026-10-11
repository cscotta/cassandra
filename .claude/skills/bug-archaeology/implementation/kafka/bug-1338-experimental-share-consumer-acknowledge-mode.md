**Commit**: 1da30bdedf · **Ticket**: KAFKA-18900
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
User testing of the `KafkaShareConsumer` interface has revealed some
areas which confuse people. One of these is that way that it decides
whether you want to use implicit or explicit acknowledgement of records
by observing which calls the application issues. We are taking the
opportunity to refine the interface before it is finalised. This PR introduces an experimental configuration called
`internal.share.acknowledgement.mode` which can be used to make the
application declare which kind of acknowledgement it wishes to use.

**Before/After**
```java
// Before
    private static final List<String> CLASSIC_PROTOCOL_UNSUPPORTED_CONFIGS = Collections.singletonList(
            GROUP_REMOTE_ASSIGNOR_CONFIG
            SESSION_TIMEOUT_MS_CONFIG
                                        CommonClientConfigs.METADATA_RECOVERY_REBOOTSTRAP_TRIGGER_MS_DOC);


// After
/**
 * The acknowledge type is used with {@link KafkaShareConsumer#acknowledge(ConsumerRecord, AcknowledgeType)} to indicate
 * whether the record was consumed successfully.
 */
    /**
     * <code>share.acknowledgement.mode</code> is being evaluated as a new configuration to control the acknowledgement mode
     * for share consumers. It will be removed or converted to a proper configuration before release.
     * An alternative being considered is <code>enable.explicit.share.acknowledgement</code> as a boolean configuration.
     */
    public static final String INTERNAL_SHARE_ACKNOWLEDGEMENT_MODE_CONFIG = "internal.share.acknowledgement.mode";
    private static final String INTERNAL_SHARE_ACKNOWLEDGEMENT_MODE_DOC = "Controls the acknowledgement mode for a share consumer." +
            " If unset, the acknowledgement mode of the consumer is decided by the method calls it uses to fetch and commit." +
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18900: experimental share consumer acknowledge mode config (#19113).

**Test**
Test modified in ShareConsumerTest.java covering the corrected behaviour.
