**Commit**: 9e809be4ce · **Ticket**: KAFKA-17529
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.utils.ConfigUtils;
    public static final String CONFIG_PROPERTIES_EXCLUDE_ALIAS_CONFIG = "config.properties.blacklist";
            .define(CONFIG_PROPERTIES_EXCLUDE_ALIAS_CONFIG,
                    Type.LIST,
                    null,
                    Importance.HIGH,
                    "Deprecated. Use " + CONFIG_PROPERTIES_EXCLUDE_CONFIG + " instead.")
            super(DEF, ConfigUtils.translateDeprecatedConfigs(props, new String[][]{
                {CONFIG_PROPERTIES_EXCLUDE_CONFIG, CONFIG_PROPERTIES_EXCLUDE_ALIAS_CONFIG}}), false);
import org.apache.kafka.common.utils.ConfigUtils;
    public static final String GROUPS_EXCLUDE_CONFIG_ALIAS = "groups.blacklist";
                    GROUPS_EXCLUDE_DOC)

// After
            super(DEF, props, false);
                    GROUPS_EXCLUDE_DOC);
            super(DEF, props, false);
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17529 remove blacklist from mm2 (#17202).

**Test**
Test modified in MirrorMakerConfigTest.java covering the corrected behaviour.
