**Commit**: dd85938661 · **Ticket**: KAFKA-18850
**Subsystem**: docs/configuration.html
**Category**: Logic error in condition

**Root cause**
Reviewers: TengYao Chi <kitingiao@gmail.com>, Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai <chia7712@apache.org>

**Before/After**
```java
// Before
      <p>To allow specific ConfigProviders, update the system property with a comma-separated list of ConfigProvider names. For example, to enable only the <b>EnvVarConfigProvider</b>, set the property as follows:</p>
      <pre><code class="language-bash">-Dorg.apache.kafka.automatic.config.providers=env</code></pre>
      <pre><code class="language-bash">-Dorg.apache.kafka.automatic.config.providers=file,env</code></pre>

// After
import org.apache.kafka.common.config.provider.EnvVarConfigProvider;
    @Test
    public void testAutomaticConfigProvidersWithFullClassName() {
        // case0: MockFileConfigProvider is disallowed by org.apache.kafka.automatic.config.providers
        System.setProperty(AbstractConfig.AUTOMATIC_CONFIG_PROVIDERS_PROPERTY, "file");
        assertThrows(ConfigException.class, () -> new TestIndirectConfigResolution(Map.of("config.providers", "file",
                "config.providers.file.class", MockFileConfigProvider.class.getName()),
                Map.of()));

        // case1: MockFileConfigProvider is allowed by org.apache.kafka.automatic.config.providers
        System.setProperty(AbstractConfig.AUTOMATIC_CONFIG_PROVIDERS_PROPERTY, MockFileConfigProvider.class.getName());
        Map<String, String> props = Map.of("config.providers", "file",
```

**How to spot**
When reviewing docs/configuration.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18850 fix the docs of org.apache.kafka.automatic.config.providers (#19039).

**Test**
Test modified in AbstractConfigTest.java covering the corrected behaviour.
