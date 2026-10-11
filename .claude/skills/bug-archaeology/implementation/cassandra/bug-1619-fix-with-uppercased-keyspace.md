# Bug 1619: Fix ConfigHelper.setInputColumnFamily with Uppercased Keyspace Name

**Commit**: 0356ee76e4 · **Ticket**: CASSANDRA-7093
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3/CqlRecordReader.java
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When establishing the Cassandra session in the Hadoop CQL3 record reader, the keyspace name was passed unquoted to `cluster.connect(keyspace)`. CQL identifiers are case-folded to lowercase unless double-quoted; a keyspace name containing uppercase letters (e.g., `"MyKeyspace"`) would therefore fail to resolve because the driver looked for `mykeyspace`. Additionally, several setter methods in `CqlConfigHelper` mistakenly used a parameter named `authProvider` for SSL path/password/cipher fields, though that was cosmetic. The real behavioral fix was quoting the keyspace identifier on connect.

### Before / After

```java
// BEFORE
session = cluster.connect(keyspace);
```

```java
// AFTER
session = cluster.connect(quote(keyspace));

private String quote(String identifier)
{
    return "\"" + identifier.replaceAll("\"", "\"\"") + "\"";
}
```

---

## How to Spot

Whenever a user-supplied identifier is passed directly into a CQL statement or connection call, it must be double-quoted (with internal quotes escaped) to preserve case and avoid conflicts with reserved words. Review every place that interpolates a keyspace, table, or column name into CQL without quoting.

---

## Test

No test added.
