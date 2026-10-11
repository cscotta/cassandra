**Commit**: 6fd951a9c0 · **Ticket**: KAFKA-17610
**Subsystem**: clients/src
**Category**: Resource leak

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    /**
     * Update the configuration for the specified resources with the default options.
     * <p>
     * This is a convenience method for {@link #alterConfigs(Map, AlterConfigsOptions)} with default options.
     * See the overload for more details.
     * <p>
     * This operation is supported by brokers with version 0.11.0.0 or higher.
     *
     * @param configs The resources with their configs (topic is the only resource type with configs that can
     *                be updated currently)
     * @return The AlterConfigsResult
     * @deprecated Since 2.3. Use {@link #incrementalAlterConfigs(Map)}.

// After
 * Options for {@link Admin#incrementalAlterConfigs(Map)}.
 * The result of the {@link Admin#incrementalAlterConfigs(Map, AlterConfigsOptions)} call.
```

**How to spot**
When reviewing clients/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17610 drop alterconfigs (#18002).

**Test**
Test modified in MockAdminClient.java covering the corrected behaviour.
