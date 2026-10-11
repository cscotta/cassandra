# Bug 2959: Bootstrap verb handler registered after bootstrap starts, missing early messages

**Commit**: 4a4b8bebf7 · **Ticket**: CASSANDRA-477

## Detection Rule
When a verb handler is registered in a sequence of `registerVerbHandlers` calls, check whether it is registered after the subsystem that sends messages using that verb is started — incoming messages arriving before registration are silently dropped.

## Code Smell
`BootstrapTokenVerbHandler` was registered after `bootstrapMetadataVerbHandler_`, `bootStrapInitiateVerbHandler_`, and others; but `startBootstrap()` was called before gossip started, meaning bootstrap token request messages could arrive before the handler was registered.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: late-verb-handler-registration
- severity: hang
- subsystem: bootstrap-startup

## Root Cause
Multiple small regressions in the bootstrap startup sequence: (1) `BootstrapTokenVerbHandler` was registered at the end of the verb-handler list rather than before bootstrap starts, so a bootstrapToken reply could arrive at an unregistered verb and be dropped. (2) Gossip was started after `startBootstrap()`, so the bootstrap node could not see load info from other nodes since they hadn't heard from it yet. (3) The load broadcaster was scheduled with a large initial delay; changing it to `2 * Gossiper.intervalInMillis_` ensures load info is broadcast as soon as the node has peers to talk to.

## Example
```java
// BEFORE: bootstrapToken handler registered too late
MessagingService.instance().registerVerbHandlers(bootstrapMetadataVerbHandler_, ...);
MessagingService.instance().registerVerbHandlers(bootStrapInitiateVerbHandler_, ...);
// ... other handlers ...
MessagingService.instance().registerVerbHandlers(bootstrapTokenVerbHandler_, ...); // too late
```
```java
// AFTER: registered first, before bootstrap starts
MessagingService.instance().registerVerbHandlers(bootstrapTokenVerbHandler_, ...);
MessagingService.instance().registerVerbHandlers(bootstrapMetadataVerbHandler_, ...);
// ... then gossip started, then bootstrap
```
