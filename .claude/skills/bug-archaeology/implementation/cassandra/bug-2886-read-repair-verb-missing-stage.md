# Bug 2886: READ_REPAIR verb not mapped to MUTATION stage, causing assertion error

**Commit**: 3d61dae55b · **Ticket**: CASSANDRA-1493

## Detection Rule
When adding a new message verb to a messaging system, check that the verb is added to every dispatch table or stage-mapping map that handles incoming messages, not only to the enum definition.

## Code Smell
`StorageService.verbStages` maps each `Verb` to a `Stage` for thread-pool routing, but `Verb.READ_REPAIR` was absent from the map. Any code that looked up the stage for an incoming `READ_REPAIR` message would receive null and throw an `AssertionError` (introduced by CASSANDRA-1465 which added the assertion).

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: verb-missing-from-dispatch-table
- severity: crash
- subsystem: network-messaging

## Root Cause
CASSANDRA-1465 added an assertion that every verb has a stage mapping. `READ_REPAIR` mutations were routed to `Stage.MUTATION` in the old code implicitly, but after the verb/stage map was made explicit, the entry was not added. The one-line fix adds `put(Verb.READ_REPAIR, Stage.MUTATION)` to the static initializer.

## Example
```java
// BEFORE — READ_REPAIR missing from map, assertion fires on first read-repair message
{{
    put(Verb.MUTATION, Stage.MUTATION);
    put(Verb.BINARY, Stage.MUTATION);
    // Verb.READ_REPAIR absent!
    put(Verb.READ, Stage.READ);
    ...
}}
```
```java
// AFTER
put(Verb.MUTATION, Stage.MUTATION);
put(Verb.BINARY, Stage.MUTATION);
put(Verb.READ_REPAIR, Stage.MUTATION);
put(Verb.READ, Stage.READ);
```
