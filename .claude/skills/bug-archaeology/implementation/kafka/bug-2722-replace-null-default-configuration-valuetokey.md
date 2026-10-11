**Commit**: 6e9c039eea · **Ticket**: KAFKA-17097
**Subsystem**: connect/transforms
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            }, ConfigDef.Importance.MEDIUM, "Field rename mappings.");
            final Object fieldValue = value.get(reverseRenamed(field.name()));
                    "Field names on the record value to extract as the record key.");
            key.put(field, value.get(field));

// After
        String REPLACE_NULL_WITH_DEFAULT_CONFIG = "replace.null.with.default";
            }, ConfigDef.Importance.MEDIUM, "Field rename mappings.")
            .define(ConfigName.REPLACE_NULL_WITH_DEFAULT_CONFIG, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM,
                    "Whether to replace fields that have a default value and that are null to the default value. When set to true, the default value is used, otherwise null is used.");
    private boolean replaceNullWithDefault;
        replaceNullWithDefault = config.getBoolean(ConfigName.REPLACE_NULL_WITH_DEFAULT_CONFIG);
            final Object fieldValue = replaceNullWithDefault ? value.get(reverseRenamed(field.name())) : value.getWithoutDefault(reverseRenamed(field.name()));
    public static final String REPLACE_NULL_WITH_DEFAULT_CONFIG = "replace.null.with.default";
                    "Field names on the record value to extract as the record key.")
            .define(REPLACE_NULL_WITH_DEFAULT_CONFIG, ConfigDef.Type.BOOLEAN, true, ConfigDef.Importance.MEDIUM,
                    "Whether to replace fields that have a default value and that are null to the default value. When set to true, the default value is used, otherwise null is used.");
    private boolean replaceNullWithDefault;
```

**How to spot**
When reviewing connect/transforms code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17097 add replace.null.with.default configuration to valuetokey and replacefield (#16571).

**Test**
Test modified in ReplaceFieldTest.java covering the corrected behaviour.
