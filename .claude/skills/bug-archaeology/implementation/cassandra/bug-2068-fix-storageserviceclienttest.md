# Bug 2068: Schema Version Updated Prematurely During initClient() Breaking StorageServiceClientTest

**Commit**: ed3215d765 · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/service/StorageService.java
**Category**: State not cleaned up

---

## Root Cause

`StorageService.initClient()` is a lightweight path used by non-server tools (and tests) that need a gossip connection without fully joining the ring. It contained a call to `Schema.instance.updateVersion()` which recalculates and broadcasts the local schema version via gossip. In the `initClient()` path no schema has been loaded yet, so the version computed and announced was wrong, causing `StorageServiceClientTest` to see an unexpected schema-version change during initialization. Removing the spurious call restores the invariant that `updateVersion()` is only called after schema tables are properly populated.

### Before / After

```java
// BEFORE – schema version computed with empty schema
Gossiper.instance.addLocalApplicationState(ApplicationState.NET_VERSION, valueFactory.networkVersion());
Schema.instance.updateVersion();  // wrong: schema not yet loaded
MessagingService.instance().listen(FBUtilities.getLocalAddress());
```

```java
// AFTER – spurious call removed
Gossiper.instance.addLocalApplicationState(ApplicationState.NET_VERSION, valueFactory.networkVersion());
MessagingService.instance().listen(FBUtilities.getLocalAddress());
```

---

## How to Spot

Any call to a "broadcast state" or "update version" method in an initialization path should be audited to confirm the underlying data it reads is already fully populated. In thin-client or test init paths it is common for schema/state loading to be skipped, making such calls both incorrect and misleading to consumers.

---

## Test

No test added (the fix itself restores a previously passing test).
