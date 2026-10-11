# Bug 1679: Fix Broken Build — Duplicate switch Case in StorageService

**Commit**: 97a6e36bcd · **Ticket**: n/a
**Subsystem**: service/StorageService
**Category**: Logic error in condition

---

## Root Cause

A prior commit left a `switch` statement in `StorageService.onChange()` with two `case SCHEMA:` labels. The first one stored the schema version to the peer table and broke out, and the second one (at the bottom of the switch) called `MigrationManager.instance.scheduleSchemaPull()`. Having two branches for the same case is a compile error in Java. The fix merges the correct action (`scheduleSchemaPull`) into the first `case SCHEMA:` block and removes the duplicate label, so both the table update and the schema pull happen together.

### Before / After

```java
// BEFORE (two case SCHEMA: labels — does not compile)
case SCHEMA:
    SystemTable.updatePeerInfo(endpoint, "schema_version", value.value);
    break;
case HOST_ID:
    SystemTable.updatePeerInfo(endpoint, "host_id", value.value);
    break;
case SCHEMA:
    MigrationManager.instance.scheduleSchemaPull(endpoint, epState);
```

```java
// AFTER
case SCHEMA:
    SystemTable.updatePeerInfo(endpoint, "schema_version", value.value);
    MigrationManager.instance.scheduleSchemaPull(endpoint, epState);
    break;
case HOST_ID:
    SystemTable.updatePeerInfo(endpoint, "host_id", value.value);
    break;
```

---

## How to Spot

Duplicate `case` labels in a `switch` block are a compile-time error in Java and will be caught immediately at build time. During code review, look for split logic where one case handles storage and another handles a follow-up action for the same enum value — these should almost always be combined into one case.

---

## Test

No test added.
