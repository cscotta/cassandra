**Commit**: e9ccc2d6f5 · **Ticket**: KAFKA-16041
**Subsystem**: LICENSE-binary
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
jackson-module-afterburner-2.16.2
    api libs.jacksonAfterburner
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

// After
jackson-module-blackbird-2.16.2
    api libs.jacksonBlackbird
     * @param enableBlackbird permits to enable/disable the registration of Jackson Blackbird module.
    public JsonConverter(boolean enableBlackbird) {
            enableBlackbird
            enableBlackbird
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
        final boolean enableBlackbird
        if (enableBlackbird) {
            objectMapper.registerModule(new BlackbirdModule());
import com.fasterxml.jackson.module.blackbird.BlackbirdModule;
        final boolean enableBlackbird
```

**How to spot**
When reviewing LICENSE-binary code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16041: replace afterburn module with blackbird (#17884).

**Test**
No test added.
