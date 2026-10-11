# Bug 0530: Tracing and Repair Schema Columns Break 3.x/4.0 Mixed Mode Connections

**Commit**: 9b1f3796a6 · **Ticket**: CASSANDRA-15385
**Subsystem**: `src/java/org/apache/cassandra/tracing/TraceKeyspace.java`, `src/java/org/apache/cassandra/repair/SystemDistributedKeyspace.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

The 4.0 branch added new columns (`coordinator_port`, `source_port`, `participants_v2`, `options`) to the `system_traces` and `system_distributed` schema definitions. When a 4.0 node writes tracing or repair records using these columns while a 3.x node is still in the cluster, the 3.x node attempts to deserialize rows it does not understand, causing connection failures. The 4.0 columns should only be used when all nodes in the cluster support them, but the schema definitions in `TraceKeyspace` and `SystemDistributedKeyspace` were not made conditional on the cluster version.

### Before / After

```java
// BEFORE (TraceKeyspace - 3.x definition, missing new columns)
+ "coordinator inet,"
+ "duration int,"
```

```java
// AFTER (3.x definition now includes the 4.0 columns as forward-compatible stubs)
+ "coordinator inet,"
+ "coordinator_port int,"
+ "duration int,"
// similarly source_port in events, participants_v2/options in repair
```

---

## How to Spot

Any time new columns are added to internal system keyspace table definitions in a major version, check whether the schema is written by nodes that may still be communicating with older-version peers. Forward-compatible schema additions should be present in the older branch's definition so that 3.x nodes can at least ignore the extra columns without breaking the connection.

---

## Test

No test added.
