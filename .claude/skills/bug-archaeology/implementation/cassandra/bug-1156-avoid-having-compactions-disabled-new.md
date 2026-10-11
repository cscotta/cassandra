# Bug 1156: Wrong Method Call Re-enables Compaction on Tables Where It Was JMX-Disabled

**Commit**: b724d1ea51 · **Ticket**: CASSANDRA-9963
**Subsystem**: src/java/org/apache/cassandra/db/compaction/WrappingCompactionStrategy.java
**Category**: Logic error in condition

---

## Root Cause

`WrappingCompactionStrategy.reloadCompactionStrategy` computed whether compaction had been disabled via JMX by calling `!isEnabled()` rather than reading the raw `enabled` field directly. `isEnabled()` delegates to the inner strategy objects (`repaired` and `unrepaired`), which are about to be replaced. On a brand-new table the inner strategies are `null`, so `isEnabled()` returned `false`, making `!isEnabled()` evaluate to `true` — falsely indicating that compaction had been JMX-disabled. The fix reads `!enabled` (the backing boolean field) before the strategies are torn down, which is always an accurate reflection of the JMX-controlled state.

### Before / After

```java
// BEFORE – delegates to inner strategies; returns wrong result when strategies are null
boolean disabledWithJMX = !isEnabled() && shouldBeEnabled();
```

```java
// AFTER – reads the authoritative backing field directly
boolean disabledWithJMX = !enabled && shouldBeEnabled();
```

---

## How to Spot

When a flag's setter stores state in a field but its getter reads derived state from child objects, calling the getter at a point when those children are absent or being replaced will silently return the wrong value. Reviewers should prefer reading the authoritative backing field when the question is "what did the operator explicitly configure?" rather than "what is the current runtime state?", especially in reload or replace sequences where child objects may be transiently null.

---

## Test

`test/unit/org/apache/cassandra/db/compaction/CompactionsCQLTest.java` — verifies that compaction is enabled on a freshly created table and that minor compaction actually fires after multiple flushes.
