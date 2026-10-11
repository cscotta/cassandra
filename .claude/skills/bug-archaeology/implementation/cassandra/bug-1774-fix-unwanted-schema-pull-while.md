# Bug 1774: Fix Unwanted Schema Pull While Upgrading Nodes from 1.2

**Commit**: 31414fb871 · **Ticket**: CASSANDRA-6678
**Subsystem**: src/java/org/apache/cassandra/service/MigrationManager.java
**Category**: Missing null / bounds check

---

## Root Cause

This is the same fix as commit 9e0700c07e applied to a different branch of the same method. On this branch the lower-bound version check `>= VERSION_117` was also present, but `getVersion(endpoint)` was still called without a `knowsVersion` guard. During a rolling upgrade from 1.2 to 2.0, an endpoint that had not yet exchanged a messaging version could return a default value satisfying both bounds, triggering an incompatible schema pull. Adding `knowsVersion(endpoint)` as the first predicate prevents evaluation of the version range until a real version is on record.

### Before / After

```java
// BEFORE – both bounds checked but no existence guard
return MessagingService.instance().getVersion(endpoint) >= MessagingService.VERSION_117
        && MessagingService.instance().getVersion(endpoint) <= MessagingService.current_version
        && !Gossiper.instance.isFatClient(endpoint);
```

```java
// AFTER – existence guard added as first predicate
return MessagingService.instance().knowsVersion(endpoint)
        && MessagingService.instance().getVersion(endpoint) >= MessagingService.VERSION_117
        && MessagingService.instance().getVersion(endpoint) <= MessagingService.current_version
        && !Gossiper.instance.isFatClient(endpoint);
```

---

## How to Spot

Any lookup that returns a default or zero value for unknown keys must be guarded by an existence check before the returned value is used in a comparison. Reviewers should look for `getVersion(x) <= BOUND` patterns and ask whether the map will always contain an entry for `x` at the call site.

---

## Test

No test added.
