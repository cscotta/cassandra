**Commit**: c37b14547d · **Ticket**: KAFKA-20357
**Subsystem**: server/src
**Category**: Logic error in condition

**Root cause**
Unify method signature for retrieving config names across config
classes. Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
import java.util.HashSet;
            ConfigResource.Type.TOPIC, new SetContainsPredicate(new HashSet<>(LogConfig.configNames())),
            ConfigResource.Type.CLIENT_METRICS, new SetContainsPredicate(ClientMetricsConfigs.configDef().names()),
            ConfigResource.Type.GROUP, new SetContainsPredicate(GroupConfig.CONFIG_DEF.names())
    public static Set<String> names() {
            if (!names().contains(key)) {
    public static List<String> configNames() {
        return CONFIG.names().stream().sorted().toList();
        List<String> names = configNames();

// After
            ConfigResource.Type.TOPIC, new SetContainsPredicate(LogConfig.configNames()),
            ConfigResource.Type.CLIENT_METRICS, new SetContainsPredicate(ClientMetricsConfigs.configNames()),
            ConfigResource.Type.GROUP, new SetContainsPredicate(GroupConfig.configNames())
    public static Set<String> configNames() {
            if (!configNames().contains(key)) {
    public static Set<String> configNames() {
        return CONFIG.names();
        Set<String> names = configNames();
```

**How to spot**
When reviewing server/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20357 unify method signature for retrieving config names across config classes (#21951).

**Test**
No test added.
