**Commit**: 0ef1394a60 · **Ticket**: KAFKA-19112
**Subsystem**: connect/runtime
**Category**: Logic error in condition

**Root cause**
[KAFKA-19112](https://github.com/apache/kafka/pull/20334) defines the
`plugin.path` config as non-empty. This PR add validation for
`plugin.path` related config in the `connect-plugin-path.sh` tool to
satisfy the same non-empty semantics, addressing the issue [mentioned
here](https://github.com/apache/kafka/pull/20334/files#r2395222944). Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
                if (pluginPath.isEmpty()) {
                    log.warn("Plugin path element is empty, evaluating to {}.", pluginPathElement);
                }
        } catch (TerseException e) {

// After
import org.apache.kafka.common.config.ConfigException;
import java.util.regex.Pattern;
    private static final Pattern COMMA_WITH_WHITESPACE = Pattern.compile("\\s*,\\s*");

        } catch (TerseException | ConfigException e) {
        for (String pluginPath : rawPluginPaths) {
            validatePluginPath(pluginPath, "--plugin-path");
        }
                validatePluginPath(pluginPath, WorkerConfig.PLUGIN_PATH_CONFIG);
    private static void validatePluginPath(String pluginPath, String configName) throws ConfigException {
        String trimmed = pluginPath.trim();
        if (trimmed.isEmpty()) {
```

**How to spot**
When reviewing connect/runtime code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: add plugin.path config non-empty check in connect-plugin-path.sh (#20638).

**Test**
Test modified in ConnectPluginPathTest.java covering the corrected behaviour.
