# Bug 1764: Stress JavaDriverClient Metrics Conflict and Wrong Shutdown API

**Commit**: 0d0acac6c5 · **Ticket**: n/a
**Subsystem**: tools/stress/util/JavaDriverClient
**Category**: Wrong constant / default value

---

## Root Cause

The stress tool's `JavaDriverClient` built its Cassandra Java driver `Cluster` without disabling the driver's built-in metrics. The driver (version 2.0.0-rc3) uses a version of the Metrics library that conflicted with the version used by Cassandra itself, causing classpath clashes at runtime. Additionally, `disconnect()` called `cluster.shutdown()` which was removed in the updated driver version; the correct replacement API is `cluster.close()`.

### Before / After

```java
// BEFORE
Cluster.Builder clusterBuilder = Cluster.builder()
        .addContactPoint(host).withPort(port);
// ...
FBUtilities.waitOnFuture(cluster.shutdown());
```

```java
// AFTER
Cluster.Builder clusterBuilder = Cluster.builder()
                                        .addContactPoint(host)
                                        .withPort(port)
                                        .withoutMetrics(); // driver uses metrics 3, conflicts with our version
// ...
cluster.close();
```

---

## How to Spot

When upgrading a third-party driver dependency, check whether its default configuration enables any subsystems (metrics, logging, JMX) that pull in transitive library versions — these can silently conflict with the host application's own dependencies. Also verify that all API call sites are updated when methods are renamed or replaced across driver versions.

---

## Test

No test added.
