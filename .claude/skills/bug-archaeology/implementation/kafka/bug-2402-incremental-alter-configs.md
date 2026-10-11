**Commit**: 9242723e4c · **Ticket**: KAFKA-17435
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    @Deprecated
    public static final String USE_INCREMENTAL_ALTER_CONFIGS = "use.incremental.alter.configs";
    private static final String USE_INCREMENTAL_ALTER_CONFIG_DOC = "Deprecated. Which API to use for syncing topic configs. " +
            "The valid values are 'requested', 'required' and 'never'. " +
            "By default, set to 'requested', which means the IncrementalAlterConfigs API is being used for syncing topic configurations " +
            "and if any request receives an error from an incompatible broker, it will fallback to using the deprecated AlterConfigs API. " +
            "If explicitly set to 'required', the IncrementalAlterConfigs API is used without the fallback logic and +" +
            "if it receives an error from an incompatible broker, the connector will fail." +
            "If explicitly set to 'never', the AlterConfig is always used." +
            "This setting will be removed and the behaviour of 'required' will be used in Kafka 4.0, therefore users should ensure that target broker is at least 2.3.0";
    public static final String REQUEST_INCREMENTAL_ALTER_CONFIGS = "requested";
    public static final String REQUIRE_INCREMENTAL_ALTER_CONFIGS = "required";

// After
// (no additions)
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17435 remove use.incremental.alter.configs (#17027).

**Test**
Test modified in MirrorSourceConnectorTest.java covering the corrected behaviour.
