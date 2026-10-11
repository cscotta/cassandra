# Bug 0542: nodetool tablehistograms Silent Failure for Non-Existent Table

**Commit**: d6c049f083 · **Ticket**: CASSANDRA-14410
**Subsystem**: `src/java/org/apache/cassandra/tools/nodetool/TableHistograms.java`
**Category**: Missing null / bounds check

---

## Root Cause

`TableHistograms` built its list of tables from user-provided arguments but never validated those arguments against the set of tables actually known to the node. When the user specified a keyspace/table that did not exist, the subsequent JMX calls returned null metric arrays; the code then called `ArrayUtils.addAll` on those null arrays and passed them to `EstimatedHistogram`, silently producing incorrect (all-zero) output instead of an actionable error. The refactoring also improved the collection type from `HashMap<String, List<String>>` to a `HashMultimap`, removing an intermediate `ArrayList` allocation per keyspace.

### Before / After

```java
// BEFORE – no existence check; JMX returns null for unknown tables
tablesList.put(args.get(0),
    new ArrayList<>(Arrays.asList(args.get(1))));
// ... proceeds to use null metric arrays
```

```java
// AFTER – pre-build allTables, then validate
for (String keyspace : tablesList.keys())
    for (String table : tablesList.get(keyspace))
        if (!allTables.containsEntry(keyspace, table))
            throw new IllegalArgumentException(
                "Unknown table " + keyspace + '.' + table);
```

---

## How to Spot

CLI commands that accept resource names (keyspace, table, index) and then look up those names via JMX should always validate inputs against the authoritative list before performing further operations. When JMX getters can return null for missing resources and the return value is used without a null check, the symptom is garbled output rather than a clear error—a reliable sign that an existence check is absent.

---

## Test

No test added.
