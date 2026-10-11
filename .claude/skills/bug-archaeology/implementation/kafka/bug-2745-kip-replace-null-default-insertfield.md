**Commit**: e2ec389a7c · **Ticket**: KAFKA-15838
**Subsystem**: connect/transforms
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
                    + "int64, float32, float64, boolean, and string. Note that binary fields can only be cast to string.");
            final Object origFieldValue = value.get(field);
        new ConfigDef()
            .define(
                FIELD_CONFIG,
                ConfigDef.Type.STRING,
                ConfigDef.NO_DEFAULT_VALUE,
                ConfigDef.Importance.MEDIUM,
                "Field name to extract."
            ));

// After
    public static final String REPLACE_NULL_WITH_DEFAULT_CONFIG = "replace.null.with.default";
                    + "int64, float32, float64, boolean, and string. Note that binary fields can only be cast to string.")
            .define(REPLACE_NULL_WITH_DEFAULT_CONFIG,
                    ConfigDef.Type.BOOLEAN,
                    true,
                    ConfigDef.Importance.MEDIUM,
                    "Whether to replace fields that have a default value and that are null to the default value. When set to true, the default value is used, otherwise null is used.");
    private boolean replaceNullWithDefault;
        replaceNullWithDefault = config.getBoolean(REPLACE_NULL_WITH_DEFAULT_CONFIG);
            final Object origFieldValue = getFieldValue(value, field);
    private Object getFieldValue(Struct value, Field field) {
        if (replaceNullWithDefault) {
```

**How to spot**
When reviewing connect/transforms code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15838: kip-1040: add replace.null.with.default to insertfield, extractfield, headerfrom, cast, setschemametadata, .

**Test**
Test modified in CastTest.java covering the corrected behaviour.
