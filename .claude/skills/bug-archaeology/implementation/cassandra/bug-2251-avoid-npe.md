# Bug 2251: NPE in CQL3 Batch Statements from Direct Field Access on ModificationStatement

**Commit**: c20b33c937 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/cql3/statements/ModificationStatement.java, BatchStatement.java, DeleteStatement.java, UpdateStatement.java
**Category**: Missing null / bounds check

---

## Root Cause

`ModificationStatement` exposed `cLevel`, `timestamp`, and `timeToLive` as `protected` fields, and subclasses accessed them directly. In `BatchStatement`, when propagating a batch timestamp to child statements, the code set `statement.timestamp = timestamp` using the raw field — but `timestamp` at that point was an unresolved `Long` that could be null if the batch had no explicit timestamp set. The `getTimestamp(clientState)` accessor handles the null case by falling back to `clientState.getTimestamp()`, so bypassing it caused NPEs when the batch timestamp was absent.

The fix encapsulates all three fields as `private`, adds a `setTimestamp(long)` mutator, and makes all access go through the safe accessor methods, eliminating the direct-field NPE path.

### Before / After

```java
// BEFORE
protected Long timestamp;  // publicly writable, nullable
...
// BatchStatement
if (isSetTimestamp())
    statement.timestamp = timestamp;  // raw Long, may be null
```

```java
// AFTER
private Long timestamp;
public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
...
// BatchStatement
if (isSetTimestamp())
    statement.setTimestamp(getTimestamp(clientState));  // resolved long
```

---

## How to Spot

When a nullable field is accessed directly across class boundaries (especially in inheritance hierarchies), verify that all consumers go through accessor methods that handle the null case. Marking fields `private` and providing typed accessors removes the class of bugs where callers bypass null-handling logic.

---

## Test

No test added.
