**Commit**: ac08325bdd · **Ticket**: KAFKA-18047
**Subsystem**: docs/configuration.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
      <p>If users want to enable JndiLoginModule, users need to explicitly reset the system property like below. We advise the users to validate configurations and only allow trusted JNDI configurations. For more details <a href="https://nvd.nist.gov/vuln/detail/CVE-2023-25194">CVE-2023-25194</a>.

// After
      <p>If users want to enable JndiLoginModule, users need to explicitly reset the system property like below. We advise the users to validate configurations and only allow trusted JNDI configurations. For more details <a href="https://kafka.apache.org/cve-list#CVE-2023-25194">CVE-2023-25194</a>.
    <li>
      <h4><a id="org.apache.kafka.automatic.config.providers"></a><a id="systemproperties_org.apache.kafka.automatic.config.providers" href="#systemproperties_org.apache.kafka.automatic.config.providers">org.apache.kafka.automatic.config.providers</a></h4>
      <p>This system property controls the automatic loading of ConfigProvider implementations in Apache Kafka. ConfigProviders are used to dynamically supply configuration values from sources such as files, directories, or environment variables. This property accepts a comma-separated list of ConfigProvider names. By default, all built-in ConfigProviders are enabled, including <b>FileConfigProvider</b>, <b>DirectoryConfigProvider</b>, and <b>EnvVarConfigProvider</b>.</p>
      <p>If users want to disable all automatic ConfigProviders, they need to explicitly set the system property as shown below. Disabling automatic ConfigProviders is recommended in environments where configuration data comes from untrusted sources or where increased security is required. For more details, see <a href="https://kafka.apache.org/cve-list#CVE-2024-31141">CVE-2024-31141</a>.</p>
      <pre><code class="language-bash">-Dorg.apache.kafka.automatic.config.providers=none</code></pre>
      <p>To allow specific ConfigProviders, update the system property with a comma-separated list of ConfigProvider names. For example, to enable only the <b>EnvVarConfigProvider</b>, set the property as follows:</p>
      <pre><code class="language-bash">-Dorg.apache.kafka.automatic.config.providers=env</code></pre>
      <p>To use multiple ConfigProviders, include their names in a comma-separated list as shown below:</p>
      <pre><code class="language-bash">-Dorg.apache.kafka.automatic.config.providers=file,env</code></pre>
      <table>
        <tbody>
```

**How to spot**
When reviewing docs/configuration.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18047: add org.apache.kafka.automatic.config.providers to system properties section (#17920).

**Test**
No test added.
