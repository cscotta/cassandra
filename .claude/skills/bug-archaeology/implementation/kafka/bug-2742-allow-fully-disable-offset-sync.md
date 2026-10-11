**Commit**: 0781151794 · **Ticket**: KAFKA-16254
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Chris Egerton <fearthecellos@gmail.com>, Chia-Ping Tsai <chia7712@gmail.com>, Igor Soarez <soarez@apple.com>

**Before/After**
```java
// Before

    public static final String OFFSET_SYNCS_SOURCE_CONSUMER_ROLE = "offset-syncs-source-consumer";
    public static final String OFFSET_SYNCS_TARGET_CONSUMER_ROLE = "offset-syncs-target-consumer";
    public static final String OFFSET_SYNCS_SOURCE_ADMIN_ROLE = "offset-syncs-source-admin";
    public static final String OFFSET_SYNCS_TARGET_ADMIN_ROLE = "offset-syncs-target-admin";

// After
import java.util.HashMap;
    public static final String OFFSET_SYNCS_SOURCE_CONSUMER_ROLE = OFFSET_SYNCS_CLIENT_ROLE_PREFIX + "source-consumer";
    public static final String OFFSET_SYNCS_TARGET_CONSUMER_ROLE = OFFSET_SYNCS_CLIENT_ROLE_PREFIX + "target-consumer";
    public static final String OFFSET_SYNCS_SOURCE_ADMIN_ROLE = OFFSET_SYNCS_CLIENT_ROLE_PREFIX + "source-admin";
    public static final String OFFSET_SYNCS_TARGET_ADMIN_ROLE = OFFSET_SYNCS_CLIENT_ROLE_PREFIX + "target-admin";
    public static Map<String, String> validate(Map<String, String> configs) {
        Map<String, String> invalidConfigs = new HashMap<>();

        // No point to validate when connector is disabled.
        if ("false".equals(configs.getOrDefault(ENABLED, "true"))) {
            return invalidConfigs;
        }
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16254: allow mm2 to fully disable offset sync feature (#15999).

**Test**
Test modified in MirrorCheckpointConfigTest.java covering the corrected behaviour.
