# Bug 1575: Fix NPE when unknown prepared statement ID is used

**Commit**: 12c694edb6 · **Ticket**: CASSANDRA-7454
**Subsystem**: transport/messages / ExecuteMessage
**Category**: Missing null / bounds check

---

## Root Cause

`ExecuteMessage.execute()` called `handler.getPrepared(statementId)` which returns `null` when the ID is not found, then immediately called `options.prepare(prepared.boundNames)` on the null reference before checking whether the statement itself was null. The null check that did exist tested `prepared.statement` rather than `prepared` itself, so a missing prepared statement caused an NPE instead of the intended `PreparedQueryNotFoundException`.

### Before / After

```java
// BEFORE — NPE when getPrepared returns null
ParsedStatement.Prepared prepared = handler.getPrepared(statementId);
options.prepare(prepared.boundNames);  // NullPointerException if prepared == null
CQLStatement statement = prepared.statement;

if (statement == null)
    throw new PreparedQueryNotFoundException(statementId);
```

```java
// AFTER — null check immediately after lookup
ParsedStatement.Prepared prepared = handler.getPrepared(statementId);
if (prepared == null)
    throw new PreparedQueryNotFoundException(statementId);

options.prepare(prepared.boundNames);
CQLStatement statement = prepared.statement;
```

---

## How to Spot

Any method whose return value can be null should be null-checked before any field access or method call on the result. Placing a null guard on a nested field (`prepared.statement == null`) instead of the container object (`prepared == null`) is a common error that leaves the NPE path open.

---

## Test

No test added in this commit.
