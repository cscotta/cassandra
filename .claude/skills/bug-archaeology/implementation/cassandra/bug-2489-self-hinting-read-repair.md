# Bug 2489: Self-hinting of read-repair mutations — READ_REPAIR verb treated as regular MUTATION

**Commit**: 48d1284a64 · **Ticket**: CASSANDRA-3440

## Detection Rule

When a read-repair mutation is sent using the `MUTATION` verb instead of a dedicated `READ_REPAIR` verb, the expired-callback handler's `shouldHint()` logic will trigger hinted handoff for timed-out read-repair updates as if they were real client writes.

## Code Smell

`rowMutation.getMessage(Gossiper.instance.getVersion(endpoints.get(i)))` in `RowRepairResolver` constructs a message with `StorageService.Verb.MUTATION`, which triggers hint-on-timeout behavior in the expired-callback handler.

## Tags

- scope: cassandra
- category: wrong-constant
- code-pattern: wrong-verb-for-internal-mutation
- severity: performance
- subsystem: storage-engine

## Root Cause

`RowRepairResolver` called `rowMutation.getMessage(version)` which defaulted to `Verb.MUTATION`. When a read-repair timed out, the `CallbackInfo.shouldHint()` logic saw the `MUTATION` verb and scheduled a hint for the target endpoint — causing the node to hint itself for every timed-out read repair. The fix uses `rowMutation.getMessage(StorageService.Verb.READ_REPAIR, version)` so that the expired-callback handler does not schedule hints for read-repair messages.

## Example

```java
// BEFORE
repairMessage = rowMutation.getMessage(Gossiper.instance.getVersion(endpoints.get(i)));
// getMessage() defaults to Verb.MUTATION → hints scheduled on timeout
```

```java
// AFTER
repairMessage = rowMutation.getMessage(
    StorageService.Verb.READ_REPAIR,
    Gossiper.instance.getVersion(endpoints.get(i)));
// READ_REPAIR verb → shouldHint() returns false, no self-hinting
```
