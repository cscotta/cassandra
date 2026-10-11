# Bug 0761: Unnecessary system keyspace flushes on every startup

**Commit**: d6ceb940e3 · **Ticket**: CASSANDRA-13031
**Subsystem**: System keyspace / startup
**Category**: State not cleaned up

---

## Root Cause
Several `SystemKeyspace` mutators — `setViewBuilt`, `setBootstrapState`, `updatePreferredIP`, `updateTokens`, and `removeTruncationRecord` — performed unconditional writes and `forceBlockingFlush` calls every time they were invoked, even when the value being written was identical to what was already stored. On a normal restart this produced a cascade of gratuitous system table flushes that slowed startup and generated unnecessary I/O. Additionally, the `setViewRemoved` and `setIndexRemoved` paths issued `DELETE` without `IF EXISTS`, which is a no-op semantically but forced a flush and write even when the row was already gone.

### Before / After
```java
// BEFORE — setBootstrapState always writes and flushes
public static void setBootstrapState(BootstrapState state) {
    String req = "INSERT INTO system.%s (key, bootstrapped) VALUES ('%s', ?)";
    executeInternal(...);
    forceBlockingFlush(LOCAL);
}

// AFTER — guard added; also applies to setViewBuilt, updatePreferredIP,
//         updateTokens, and removeTruncationRecord
public static void setBootstrapState(BootstrapState state) {
    if (getBootstrapState() == state)
        return;
    String req = "INSERT INTO system.%s (key, bootstrapped) VALUES ('%s', ?)";
    executeInternal(...);
    forceBlockingFlush(LOCAL);
}

// BEFORE — DELETE without IF EXISTS
String buildReq = "DELETE FROM %S.%s WHERE keyspace_name = ? AND view_name = ?";

// AFTER
String buildReq = "DELETE FROM %S.%s WHERE keyspace_name = ? AND view_name = ? IF EXISTS";
```

---

## How to Spot
Look for `forceBlockingFlush` calls that are not guarded by a read-before-write equality check. Any write-then-flush sequence on a system table that is called repeatedly during startup deserves a "has anything actually changed?" guard to avoid redundant I/O and log noise.

---

## Test
No test added.
