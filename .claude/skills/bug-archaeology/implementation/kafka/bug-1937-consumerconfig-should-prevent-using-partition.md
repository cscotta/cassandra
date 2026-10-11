**Commit**: 9d23f89e05 · **Ticket**: KAFKA-17338
**Subsystem**: clients/src
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Kirk True <ktrue@confluent.io>, Chia-Ping Tsai <chia7712@gmail.com>, Lianet Magrans <lmagrans@confluent.io>

**Before/After**
```java
// Before
import java.util.Arrays;
    public static final List<String> ASSIGN_FROM_SUBSCRIBED_ASSIGNORS =
        List.of(RANGE_ASSIGNOR_NAME, ROUNDROBIN_ASSIGNOR_NAME, STICKY_ASSIGNOR_NAME, COOPERATIVE_STICKY_ASSIGNOR_NAME);
                                        Arrays.asList(RangeAssignor.class, CooperativeStickyAssignor.class),
        checkGroupRemoteAssignor();
    private void checkGroupRemoteAssignor() {
        if (getString(GROUP_PROTOCOL_CONFIG).equalsIgnoreCase(GroupProtocol.CLASSIC.name()) && getString(GROUP_REMOTE_ASSIGNOR_CONFIG) != null && !getString(GROUP_REMOTE_ASSIGNOR_CONFIG).isEmpty()) {
            throw new ConfigException(GROUP_REMOTE_ASSIGNOR_CONFIG + " cannot be set when " + GROUP_PROTOCOL_CONFIG + "=" + GroupProtocol.CLASSIC.name());

// After
import java.util.ArrayList;
    public static final List<String> ASSIGN_FROM_SUBSCRIBED_ASSIGNORS = List.of(
            RANGE_ASSIGNOR_NAME,
            ROUNDROBIN_ASSIGNOR_NAME,
            STICKY_ASSIGNOR_NAME,
            COOPERATIVE_STICKY_ASSIGNOR_NAME
    );
    /**
     * A list of configuration keys not supported for CLASSIC protocol.
     */
    private static final List<String> CLASSIC_PROTOCOL_UNSUPPORTED_CONFIGS = Collections.singletonList(
            GROUP_REMOTE_ASSIGNOR_CONFIG
```

**How to spot**
When reviewing clients/src code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17338 consumerconfig should prevent using partition assignors with consumer group protocol (#16899).

**Test**
Test modified in ConsumerConfigTest.java covering the corrected behaviour.
