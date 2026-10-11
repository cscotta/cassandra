# Bug 1645: Fix Build — Wrong Variable Name in StorageService

**Commit**: 837bded794 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service (StorageService)
**Category**: Wrong constant / default value

---

## Root Cause

After a prior refactor renamed the local variable tracking tokens that needed to be written to the system table (from `tokensToUpdateInSystemKeyspace` to `tokensToUpdateInSystemTable`), one call site in the gossip endpoint state change handler was left referencing the old name. This caused a compilation failure; tokens requiring persistence to the system table would not have been recorded had the code ever reached execution.

### Before / After

```java
// BEFORE
if (!isClientMode)
    tokensToUpdateInSystemKeyspace.add(token);
```

```java
// AFTER
if (!isClientMode)
    tokensToUpdateInSystemTable.add(token);
```

---

## How to Spot

After any variable rename, search the entire file (and all callers) for the old name to catch stale references. Compiler errors surface the immediate problem, but partial renames in large methods can leave silent logic gaps if the old name happens to refer to another in-scope variable.

---

## Test

No test added.
