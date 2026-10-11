**Commit**: db56553fd3 · **Ticket**: KAFKA-17530
**Subsystem**: connect/transforms
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.utils.ConfigUtils;
            .define("blacklist", ConfigDef.Type.LIST, null, Importance.LOW,
                    "Deprecated. Use " + ConfigName.EXCLUDE + " instead.")
            .define("whitelist", ConfigDef.Type.LIST, null, Importance.LOW,
                    "Deprecated. Use " + ConfigName.INCLUDE + " instead.")
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, ConfigUtils.translateDeprecatedConfigs(configs, new String[][]{
            {ConfigName.INCLUDE, "whitelist"},
            {ConfigName.EXCLUDE, "blacklist"},
        }));
    @Test
    public void testIncludeBackwardsCompatibility() {

// After
        final SimpleConfig config = new SimpleConfig(CONFIG_DEF, configs);
            <li>
                The <code>--whitelist</code> and <code>--blacklist</code> options were removed from the <code>org.apache.kafka.connect.transforms.ReplaceField</code>.
                Please use <code>--include</code> and <code>--exclude</code> instead.
            </li>
```

**How to spot**
When reviewing connect/transforms code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17530 remove blacklist/whitelist from replacefield (#17237).

**Test**
Test modified in ReplaceFieldTest.java covering the corrected behaviour.
