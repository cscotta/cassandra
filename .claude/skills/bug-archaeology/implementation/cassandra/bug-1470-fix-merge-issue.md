# Bug 1470: Fix Missing IMigrationListener Method Stubs After Merge

**Commit**: 50c27cca40 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/QueryProcessor.java
**Category**: Missing override of default method

---

## Root Cause

A merge introduced user-defined functions (UDFs) and added three new methods to the `IMigrationListener` interface (`onCreateFunction`, `onUpdateFunction`, `onDropFunction`). The anonymous `MigrationSubscriber` inner class in `QueryProcessor` was not updated to implement these new methods, leaving the class incomplete after the merge and causing a compilation failure.

### Before / After

```java
// BEFORE — missing the three UDF lifecycle methods
public void onCreateUserType(String ksName, String typeName) { }
public void onUpdateKeyspace(String ksName) { }
...
public void onDropUserType(String ksName, String typeName) { }
```

```java
// AFTER — all interface methods implemented
public void onCreateUserType(String ksName, String typeName) { }
public void onCreateFunction(String namespace, String functionName) { }
public void onUpdateKeyspace(String ksName) { }
...
public void onUpdateFunction(String namespace, String functionName) { }
public void onDropUserType(String ksName, String typeName) { }
public void onDropFunction(String namespace, String functionName) { }
```

---

## How to Spot

After merging branches that add methods to an interface, grep for all classes implementing that interface and verify each has stubs for every new method. IDE "implement interface" refactors and compiler errors catch this, but in large merge patches the build may not be run before committing.

---

## Test

No test added.
