**Commit**: 5d8561656c · **Ticket**: KAFKA-20110
**Subsystem**: server/src/main/java/org/apache/kafka/server/config/DynamicBrokerConfig.java
**Category**: Incorrect filtering / wrong query result

**Root cause**: `DynamicBrokerConfig.RECONFIGURABLE_CONFIGS` was populated by combining several sub-lists including `QuotaConfig.BROKER_QUOTA_CONFIGS` directly. That set was also used as the source for the HTML documentation generator, which relied on the same `configDef`. Because quota configs are dynamic-only (not in `AbstractKafkaConfig.CONFIG_DEF`), they were absent from generated broker docs. The fix introduces a named inner class `DynamicQuotaConfig.RECONFIGURABLE_CONFIGS` (same content) and adds quota config definitions explicitly to the `combined` `ConfigDef` used only for HTML generation, so docs now include quota keys.

**Before/After**:
```java
// BEFORE – quota configs absent from HTML doc output
System.out.println(configDef.toHtml(...));

// AFTER – quota configs merged into doc ConfigDef
val combined = new ConfigDef(configDef)
QuotaConfig.brokerQuotaConfigs().configKeys().forEach((_, v) => combined.define(v))
System.out.println(combined.toHtml(...))
```

**How to spot**: Documentation generators that use a singleton `ConfigDef` will silently omit any configs registered in a separate `ConfigDef` instance (e.g., dynamic-only configs). Look for `toHtml` / `toEnrichedRst` calls that reference only one `ConfigDef` object in a multi-`ConfigDef` codebase.

**Test**: No new test added. The fix was verified by regenerating the broker HTML documentation and confirming quota config entries appear.
