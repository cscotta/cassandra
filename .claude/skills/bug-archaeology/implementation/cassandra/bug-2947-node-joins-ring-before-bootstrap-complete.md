# Bug 2947: Node added to token ring before bootstrap completes

**Commit**: 2b8c1cec9a · **Ticket**: CASSANDRA-536

## Detection Rule
When a bootstrapping node calls `setToken()` before its data transfer is complete, check whether that call inserts the node into the live ring — which would cause clients to route reads to a node that does not yet have the data.

## Code Smell
`startBootstrapping` calls `setToken(token)` (which inserts into `TokenMetadata`) immediately when the bootstrap token is chosen, long before data streaming finishes; `finishBootstrapping` only gossiped STATE_NORMAL without calling `setToken`.

## Tags
- scope: cassandra
- category: logic-error
- code-pattern: premature-ring-membership
- severity: silent-wrong-result
- subsystem: bootstrap-startup

## Root Cause
`setToken()` both persists the token and registers the node in the local `TokenMetadata` ring, making it visible to replica-placement calculations. Calling it at bootstrap token selection time meant reads could be routed to the bootstrapping node before it received the data. The fix uses `SystemTable.updateToken(token)` (persist only, no ring insertion) during bootstrap setup, and moves the `setToken` call into `finishBootstrapping` which runs after all streaming completes. The startup thread then busy-waits on `isBootstrapMode` so Thrift is not enabled until bootstrap is done.

## Example
```java
// BEFORE: setToken adds node to ring immediately at token selection
setToken(BootStrapper.getBootstrapToken(...));
Gossiper.instance().addApplicationState(STATE_BOOTSTRAPPING, ...);
new BootStrapper(...).startBootstrap();
// thrift starts immediately after, serving reads from incomplete node
```
```java
// AFTER: only persist token, don't join ring until data transfer done
Token token = BootStrapper.getBootstrapToken(...);
SystemTable.updateToken(token); // DON'T use setToken
new BootStrapper(...).startBootstrap();
while (isBootstrapMode) { Thread.sleep(100); } // wait for completion
// finishBootstrapping() calls setToken() after all streaming done
```
