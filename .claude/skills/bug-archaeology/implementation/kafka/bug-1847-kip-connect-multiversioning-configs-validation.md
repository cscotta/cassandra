**Commit**: f4fe6064cc · **Ticket**: KAFKA-18215
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Greg Harris <greg.harris@aiven.io>

**Before/After**
```java
// Before
        <allow pkg="org.apache.maven.artifact.versioning" />
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
    private final ConcurrentMap<String, Connector> tempConnectors = new ConcurrentHashMap<>();

// After
      <allow pkg="org.apache.maven.artifact.versioning" />
import org.apache.kafka.connect.runtime.isolation.PluginUtils;
import org.apache.kafka.connect.runtime.isolation.VersionedPluginLoadingException;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import static org.apache.kafka.connect.runtime.ConnectorConfig.CONNECTOR_CLASS_CONFIG;
import static org.apache.kafka.connect.runtime.ConnectorConfig.CONNECTOR_VERSION;
import static org.apache.kafka.connect.runtime.ConnectorConfig.HEADER_CONVERTER_VERSION_CONFIG;
import static org.apache.kafka.connect.runtime.ConnectorConfig.KEY_CONVERTER_VERSION_CONFIG;
import static org.apache.kafka.connect.runtime.ConnectorConfig.VALUE_CONVERTER_VERSION_CONFIG;

    private final CachedConnectors cachedConnectors;
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18215: kip-891 connect multiversioning support (configs and validation changes for connectors and converters) (#17.

**Test**
Test modified in AbstractHerderTest.java covering the corrected behaviour.
