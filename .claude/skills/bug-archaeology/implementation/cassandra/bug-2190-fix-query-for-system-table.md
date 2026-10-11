# Bug 2190: System table upgrade INSERT used quoted placeholder for set literal

**Commit**: c34ecbf688 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/SystemTable.java
**Category**: Wrong serialization / deserialization

---

## Root Cause

During node upgrade, the system table migration code constructed an INSERT query using `String.format`. The `tokens` column holds a CQL `set<varchar>` value, but the format string wrapped the `%s` placeholder in single quotes (`'%s'`). Because `tokenBytes` already contains the serialized set literal (e.g. `{'token1'}`), surrounding it with quotes produced invalid CQL like `'{'token1'}'`, causing the upgrade query to fail or silently store corrupt data. Removing the surrounding quotes allows the set literal to be interpreted correctly by the CQL parser.

### Before / After

```java
// BEFORE — quotes around set literal corrupt the value
String req = "INSERT INTO system.%s (key, cluster_name, tokens, bootstrapped) VALUES ('%s', '%s', '%s', '%s')";
```

```java
// AFTER — set literal inserted unquoted
String req = "INSERT INTO system.%s (key, cluster_name, tokens, bootstrapped) VALUES ('%s', '%s', %s, '%s')";
```

---

## How to Spot

When building CQL statements via `String.format`, verify that each `%s` placeholder has the correct quoting for the target column type. Collection types (set, list, map) use their own literal syntax and must not be wrapped in single quotes; only `varchar`/`text` scalar values need quoting.

---

## Test

No test added.
