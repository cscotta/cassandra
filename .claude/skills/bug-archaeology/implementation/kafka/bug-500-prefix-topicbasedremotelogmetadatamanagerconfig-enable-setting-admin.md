**Commit**: dbc7787fcd · **Ticket**: KAFKA-19590
**Subsystem**: docs/configuration
**Category**: Logic error in condition

**Root cause**
Currently, `TopicBasedRemoteLogMetadataManager` creates the Admin client
using `commonProperties()`, which prevents users from setting
admin-specific   configurations (e.g., custom timeouts, retry policies). This commit adds a new configuration prefix
`remote.log.metadata.admin.*` to   allow separate configuration for the
Admin client, similar to the existing producer   and consumer prefixes. Reviewers: TengYao Chi <frankvicky@apache.org>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before

        try (Admin admin = Admin.create(rlmmConfig.commonProperties())) {

// After

    # Configs only for admin client
    <rlmm.prefix>.remote.log.metadata.admin.<kafka.property> = <value>
  * The new config prefix `remote.log.metadata.admin.` has been introduced. It allows independent configuration of the admin client used by `TopicBasedRemoteLogMetadataManager`. For further details, please refer to [KIP-1208](https://cwiki.apache.org/confluence/x/vYqhFg).
        try (Admin admin = Admin.create(rlmmConfig.adminProperties())) {
    public static final String REMOTE_LOG_METADATA_ADMIN_PREFIX = "remote.log.metadata.admin.";
    private Map<String, Object> adminProps;
```

**How to spot**
When reviewing docs/configuration code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19590 add prefix to topicbasedremotelogmetadatamanagerconfig to enable setting admin configs (#21277).

**Test**
Test modified in TopicBasedRemoteLogMetadataManagerConfigTest.java covering the corrected behaviour.
