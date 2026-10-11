# Bug 2095: RowMutation Eagerly Allocates HashMap Even When Modifications Map Is Supplied

**Commit**: 4b2a9c1b07 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/db/RowMutation.java
**Category**: Resource leak

---

## Root Cause

`RowMutation` declared its `modifications` field with an inline initializer (`= new HashMap<>()`), allocating a fresh `HashMap` unconditionally at object creation time. Every constructor then immediately called the three-argument constructor which reassigned `modifications` from a caller-supplied map. The eagerly allocated `HashMap` was created and immediately discarded on every call path that passed in an existing map, wasting allocation and GC pressure on a hot write path.

### Before / After

```java
// BEFORE — HashMap allocated at field initialization, then overwritten by constructor
protected Map<UUID, ColumnFamily> modifications = new HashMap<UUID, ColumnFamily>();
```

```java
// AFTER — field declared final with no initializer; assigned exactly once in the canonical constructor
private final Map<UUID, ColumnFamily> modifications;
```

---

## How to Spot

Inline field initializers on mutable collections are a red flag when any constructor delegates to another that re-assigns the same field. Prefer `final` fields with no initializer and assign only inside constructors to prevent the redundant allocation pattern.

---

## Test

No test added.
