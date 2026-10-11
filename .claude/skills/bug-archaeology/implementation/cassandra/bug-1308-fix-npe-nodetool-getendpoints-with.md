# Bug 1308: NPE in nodetool getendpoints with Invalid Keyspace or Table

**Commit**: 37eb2a0e29 · **Ticket**: CASSANDRA-8950
**Subsystem**: service/StorageService, tools/NodeCmd, tools/NodeProbe
**Category**: Missing null / bounds check

---

## Root Cause

`StorageService.getNaturalEndpoints(String keyspaceName, String cf, String key)` called `Schema.instance.getKSMetaData(keyspaceName).cfMetaData().get(cf)` as a single chained expression without checking whether `getKSMetaData` returned null (unknown keyspace) or whether the subsequent `cfMetaData().get(cf)` returned null (unknown table). When either lookup failed, the JVM threw a `NullPointerException` that propagated through the JMX boundary as an opaque error rather than a meaningful diagnostic message. The fix adds explicit null checks with `IllegalArgumentException` messages, and the tool layer (`NodeCmd.printEndPoints`) catches `IllegalArgumentException` and prints the error message to the user cleanly.

### Before / After

```java
// BEFORE – no null checks; NPE if ks or cf is invalid
CFMetaData cfMetaData = Schema.instance.getKSMetaData(keyspaceName).cfMetaData().get(cf);
return getNaturalEndpoints(keyspaceName, getPartitioner().getToken(cfMetaData.getKeyValidator().fromString(key)));
```

```java
// AFTER – explicit validation with clear error messages
KSMetaData ksMetaData = Schema.instance.getKSMetaData(keyspaceName);
if (ksMetaData == null)
    throw new IllegalArgumentException("Unknown keyspace '" + keyspaceName + "'");

CFMetaData cfMetaData = ksMetaData.cfMetaData().get(cf);
if (cfMetaData == null)
    throw new IllegalArgumentException("Unknown table '" + cf + "' in keyspace '" + keyspaceName + "'");

return getNaturalEndpoints(keyspaceName, getPartitioner().getToken(cfMetaData.getKeyValidator().fromString(key)));
```

---

## How to Spot

Any code that chains a schema/metadata lookup directly into a field access without an intermediate null check is a latent NPE. Review every call to `Schema.instance.getKSMetaData` and `cfMetaData().get(...)` and confirm that a missing keyspace or table produces a human-readable error rather than a stack trace.

---

## Test

No test added.
