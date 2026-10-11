**Commit**: 92d9dc19b2 · **Ticket**: KAFKA-19758
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
…ugins if it has the correct version

Reviewers: Greg Harris <gharris1727@gmail.com>, Fiore Mario Vitale <mvitale86@gmail.com>, Snehashis Pal <snehashisp1995@gmail.com>

**Before/After**
```java
// Before
              files="DistributedHerderTest.java"/>
            connectorLoader = plugins().pluginLoader(connType, connVersion);
            return (T) plugins.newPlugin(getClass(classConfig).getName(), range, plugins.pluginLoader(getString(CONNECTOR_CLASS_CONFIG), connectorRange));
            return plugins.pluginVersion(pluginName, plugins.pluginLoader(connectorClass, range), pluginType);
                plugin = (T) plugins.newPlugin(pluginClass, pluginVersion, plugins.pluginLoader(connectorClass, connectorVersionRange));
            return plugins.pluginLoader(klass, PluginUtils.connectorVersionRequirement(version));

// After
              files="(AbstractHerderTest|DistributedHerderTest).java"/>
            connectorLoader = plugins().connectorLoader(connType, connVersion);
            return (T) plugins.newPlugin(getClass(classConfig).getName(),
                                         range,
                                         plugins.connectorLoader(getString(CONNECTOR_CLASS_CONFIG), connectorRange));
            return plugins.pluginVersion(pluginName, plugins.connectorLoader(connectorClass, range), pluginType);
                plugin = (T) plugins.newPlugin(pluginClass, pluginVersion, plugins.connectorLoader(connectorClass, connectorVersionRange));
            return plugins.connectorLoader(klass, PluginUtils.connectorVersionRequirement(version));
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19758: preferably use the connector classloader when loading pl… (#20675).

**Test**
Test modified in AbstractHerderTest.java covering the corrected behaviour.
