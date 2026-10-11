# Bug 1830: cfstats Fails on Index Column Families Due to Wrong JMX MBean Type

**Commit**: 41325c346a · **Ticket**: CASSANDRA-6406
**Subsystem**: src/java/org/apache/cassandra/tools
**Category**: Wrong constant / default value

---

## Root Cause

`NodeProbe.getColumnFamilyMetric()` always constructed the JMX `ObjectName` with `type=ColumnFamily`, but secondary index column families are registered under `type=IndexColumnFamily`. When `cfstats` was run against a table that had secondary indexes, the hardcoded `ColumnFamily` type caused the JMX lookup to fail because no MBean existed at that name. The fix detects whether the CF name contains a dot (the index indicator) and selects the correct MBean type accordingly.

### Before / After

```java
// BEFORE
new ObjectName("org.apache.cassandra.metrics:type=ColumnFamily,keyspace=" + ks + ",scope=" + cf + ",name=" + metricName)
```

```java
// AFTER
String type = cf.contains(".") ? "IndexColumnFamily" : "ColumnFamily";
ObjectName oName = new ObjectName(
    String.format("org.apache.cassandra.metrics:type=%s,keyspace=%s,scope=%s,name=%s", type, ks, cf, metricName));
```

---

## How to Spot

When constructing JMX or metrics object names with a hardcoded type string, check whether multiple MBean registration types exist in the codebase for the same metric category. The pattern of building the `ObjectName` inline in a switch statement hides the assumption that there is only one type.

---

## Test

No test added.
