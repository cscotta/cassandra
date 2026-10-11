**Commit**: 3f43d27483 · **Ticket**: KAFKA-17111
**Subsystem**: connect/json
**Category**: Wrong serialization / deserialization

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, Josep Prat <josep.prat@aiven.io>, Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
     * @param enableModules permits to enable/disable the registration of additional Jackson modules.
    public JsonConverter(boolean enableModules) {
            enableModules
            enableModules
        final boolean enableModules
        if (enableModules) {
            objectMapper.findAndRegisterModules();
        final boolean enableModules
        if (enableModules) {
            objectMapper.findAndRegisterModules();

// After
     * @param enableAfterburner permits to enable/disable the registration of Jackson Afterburner module.
    public JsonConverter(boolean enableAfterburner) {
            enableAfterburner
            enableAfterburner
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
        final boolean enableAfterburner
        if (enableAfterburner) {
            objectMapper.registerModule(new AfterburnerModule());
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
        final boolean enableAfterburner
        if (enableAfterburner) {
            objectMapper.registerModule(new AfterburnerModule());
```

**How to spot**
When reviewing connect/json code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17111: explicitly register afterburner module in jsonserializer and jsondeserializer (#16565).

**Test**
No test added.
