# Bug 1773: Fix Unwanted Schema Pull While Upgrading Nodes from 1.2

**Commit**: 9e0700c07e · **Ticket**: CASSANDRA-6678
**Subsystem**: src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Missing null / bounds check

---

## Root Cause

`MigrationManager.shouldPullSchemaFrom(endpoint)` called `MessagingService.getVersion(endpoint)` without first verifying that a version had been negotiated with that endpoint. When a 1.2 node was being upgraded and had not yet completed its handshake, `getVersion()` could return a default or sentinel value that happened to satisfy the version-range check, causing the upgrading node to attempt an incompatible schema exchange. The fix adds a `knowsVersion(endpoint)` guard before inspecting the version number, so schema pulls are only considered for endpoints whose protocol version is actually known.

### Before / After

```java
// BEFORE – version check on an unknown endpoint may pass spuriously
return MessagingService.instance().getVersion(endpoint) <= MessagingService.current_version
        && !Gossiper.instance.isFatClient(endpoint);
```

```java
// AFTER – guard against unknown version before comparing it
return MessagingService.instance().knowsVersion(endpoint)
        && MessagingService.instance().getVersion(endpoint) <= MessagingService.current_version
        && !Gossiper.instance.isFatClient(endpoint);
```

---

## How to Spot

Any lookup that returns a default or zero value for unknown keys must be guarded by an existence check before the returned value is used in a comparison. Reviewers should look for `getVersion(x) <= BOUND` patterns and ask whether the map will always contain an entry for `x` at the call site.

---

## Test

No test added.
