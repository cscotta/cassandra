# Bug 1966: CAS consistency levels passed in wrong order to StorageProxy

**Commit**: 2548e5aca3 · **Ticket**: n/a (follow-up to CASSANDRA-5797)
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java
**Category**: Wrong constant / default value

---

## Root Cause

`ModificationStatement.executeWithCondition` called `StorageProxy.cas()` with the two consistency-level arguments transposed: `options.getConsistency()` was passed where `serialConsistency` was expected, and `options.getSerialConsistency()` was passed where the commit-phase `consistency` was expected. This meant CAS operations would use the regular consistency level for the Paxos serial phase and the serial consistency level for the commit phase, producing incorrect or unpredictable behaviour for lightweight transactions.

### Before / After

```java
// BEFORE – arguments transposed
ColumnFamily result = StorageProxy.cas(keyspace(), columnFamily(), key, clusteringPrefix,
    expected, updates,
    options.getConsistency(),        // wrong: should be serialConsistency
    options.getSerialConsistency()); // wrong: should be consistency
```

```java
// AFTER – correct order
ColumnFamily result = StorageProxy.cas(keyspace(), columnFamily(), key, clusteringPrefix,
    expected, updates,
    options.getSerialConsistency(),  // Paxos serial consistency
    options.getConsistency());       // commit-phase consistency
```

---

## How to Spot

When two parameters share similar names (e.g., `consistency` and `serialConsistency`), they are easy to swap at call sites, especially when both are the same type. Review every call site of methods with multiple same-typed parameters of similar semantic — use named-parameter patterns or wrapper types when the language supports it to make transposition impossible.

---

## Test

No test added.
