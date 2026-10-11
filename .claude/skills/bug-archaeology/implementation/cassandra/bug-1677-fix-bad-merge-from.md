# Bug 1677: Fix Bad Merge of CASSANDRA-6961 From 2.0 to 2.1

**Commit**: 76db70e9d4 · **Ticket**: CASSANDRA-6961
**Subsystem**: service/StorageService
**Category**: Logic error in condition

---

## Root Cause

When CASSANDRA-6961 was merged from the 2.0 branch into 2.1, the merge produced a `prepareToJoin()` method with two parallel code paths: one outside the `if (!joined)` guard (which set `joined = true` and started gossip prematurely) and one inside it that also started gossip. The outer path ran unconditionally, calling the old 2.0 bootstrap/replace logic and setting up gossip before the 2.1 guard could take effect. This resulted in duplicate initialisation and the wrong RPC address being advertised (`DatabaseDescriptor.getRpcAddress()` instead of `getBroadcastRpcAddress()`). The fix removes the duplicate outer block entirely, leaving only the correct guarded path with the proper `getBroadcastRpcAddress()` call.

### Before / After

```java
// BEFORE (outside the guard — leftover from bad merge)
joined = true;
...
if (DatabaseDescriptor.isReplacing()) {
    ...
    appStates.put(ApplicationState.STATUS, valueFactory.hibernate(true));
} else if (shouldBootstrap() && !joined) {
    ...
    appStates.put(ApplicationState.RPC_ADDRESS,
        valueFactory.rpcaddress(DatabaseDescriptor.getRpcAddress())); // wrong address
    ...
    Gossiper.instance.start(...);
}
// and then the real block below...
if (!joined) { ... }
```

```java
// AFTER — single guarded block only
if (!joined) {
    ...
    appStates.put(ApplicationState.RPC_ADDRESS,
        valueFactory.rpcaddress(DatabaseDescriptor.getBroadcastRpcAddress())); // correct
    Gossiper.instance.start(...);
    ...
}
```

---

## How to Spot

After merging a feature branch, check for duplicated logic blocks that differ only in minor details (e.g., one using a local variable, one a method call). Merge conflicts in initialisation methods are especially prone to leaving duplicate setup sequences that silently run twice.

---

## Test

No test added.
