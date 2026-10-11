# Bug 0161: User-Defined Functions Resolved Against Live Schema During TCM Log Replay, Exposing Intermediate State

**Commit**: 5d4bcc797a · **Ticket**: CASSANDRA-19384
**Subsystem**: src/java/org/apache/cassandra/tcm/log, src/java/org/apache/cassandra/cql3/functions
**Category**: State not cleaned up

---

## Root Cause

When Cassandra replays the Transaction Cluster Metadata (TCM) log at startup, each schema transformation is applied in sequence to rebuild cluster state. During this replay, `FunctionResolver.get` called `Schema.instance.getUserFunctions()` to look up user-defined functions (UDFs). `Schema.instance` always reflects the *live, fully-initialized* schema rather than the partially-replayed intermediate state being reconstructed. This meant that UDFs created in later log entries (or not yet applied) could be visible during the preparation of earlier entries, and UDFs that were dropped could still appear. The fix removes the `Schema.instance.getUserFunctions()` calls from `FunctionResolver` and instead passes the snapshot of `UserFunctions` derived from the metadata being processed (`UserFunctions.getCurrentUserFunctions(...)` reads from `ClusterMetadata.current()`, the in-progress replay state). Separately, MBeans were registered at construction time before the log was replayed, making JMX state visible before startup completed; `registerMBeans()` is now called as an `afterReplay` callback once the log is fully applied.

### Before / After

```java
// BEFORE – FunctionResolver reads from live Schema during replay
candidates.addAll(Schema.instance.getUserFunctions(name));
```

```java
// AFTER – caller passes functions from current (in-replay) metadata
Function fun = FunctionResolver.get(keyspace, name, terms, receiver.ksName, receiver.cfName,
                                    receiver.type,
                                    UserFunctions.getCurrentUserFunctions(name, keyspace));
```

```java
// BEFORE – MBeans registered at StorageService construction
registerMBeans();   // called in constructor, before log replay
```

```java
// AFTER – MBeans registered only after replay completes
.afterReplay(Startup::scrubDataDirectories,
             (metadata) -> StorageService.instance.registerMBeans())
```

---

## How to Spot

Any code that reads from a global singleton (schema registry, service locator, cache) during a sequential replay or migration phase may observe a state that is ahead of or behind the point being reconstructed. During review, look for calls to `Schema.instance`, `ClusterMetadata.current()`, or similar singletons inside transformation `execute()` or `apply()` methods and verify that they either read from the passed-in metadata argument or are explicitly documented as safe to read from the live state at that point.

---

## Test

No new dedicated test added; existing `CQLTester` harness updated to call `registerMBeans()` during test initialization to match the corrected startup ordering.
