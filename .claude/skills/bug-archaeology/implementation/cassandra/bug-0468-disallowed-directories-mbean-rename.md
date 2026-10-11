# Bug 0468: BlacklistedDirectories Renamed to DisallowedDirectories, Old MBean Name Not Retained

**Commit**: c8c3c269bf · **Ticket**: CASSANDRA-15862
**Subsystem**: `src/java/org/apache/cassandra/db/DisallowedDirectories.java`
**Category**: State not cleaned up

---

## Root Cause

The `BlacklistedDirectories` class was renamed to `DisallowedDirectories` to modernize its nomenclature, but the original JMX MBean registration only used the new name `org.apache.cassandra.db:type=DisallowedDirectories`. Operators and monitoring tools using the old MBean name `org.apache.cassandra.db:type=BlacklistedDirectories` would silently lose access to the disk-error management interface after the upgrade. The fix registers both the deprecated legacy name and the new name simultaneously, preserving backward compatibility.

### Before / After

```java
// BEFORE (BlacklistedDirectories)
public static final String MBEAN_NAME = "org.apache.cassandra.db:type=BlacklistedDirectories";
private BlacklistedDirectories() {
    MBeanWrapper.instance.registerMBean(this, MBEAN_NAME, MBeanWrapper.OnException.LOG);
}
```

```java
// AFTER (DisallowedDirectories)
public static final String DEPRECATED_MBEAN_NAME = "org.apache.cassandra.db:type=BlacklistedDirectories";
public static final String MBEAN_NAME = "org.apache.cassandra.db:type=DisallowedDirectories";
private DisallowedDirectories() {
    MBeanWrapper.instance.registerMBean(this, DEPRECATED_MBEAN_NAME, MBeanWrapper.OnException.LOG);
    MBeanWrapper.instance.registerMBean(this, MBEAN_NAME, MBeanWrapper.OnException.LOG);
}
```

---

## How to Spot

Any time a JMX MBean class is renamed or its bean name constant is changed, the old name must be registered alongside the new one to avoid breaking existing monitoring or operations tooling. A reviewer should search for all external references to the old MBean name (scripts, dashboards, other classes) and verify that either a backward-compatible alias is registered or a migration guide is provided.

---

## Test

No test added.
