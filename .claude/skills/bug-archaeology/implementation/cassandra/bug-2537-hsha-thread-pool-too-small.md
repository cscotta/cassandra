# Bug 2537: hsha Thrift server default max thread pool too small (= CPU cores)

**Commit**: 90d4c337bf · **Ticket**: n/a

## Detection Rule
When the default thread pool size for a half-synchronous/half-asynchronous server is set to `availableProcessors()`, verify that this is sufficient for the expected I/O wait time; hsha servers need a multiplier because threads block on I/O.

## Code Smell
`conf.rpc_max_threads = Runtime.getRuntime().availableProcessors()` for the hsha server — a pool equal to the number of CPU cores will saturate immediately under any I/O-bound load, queuing requests instead of processing them concurrently.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: thread-pool-too-small-default
- severity: performance
- subsystem: network-messaging

## Root Cause
The hsha (half-sync/half-async) Thrift server processes requests on a thread pool. The default was set to `availableProcessors()`, which only makes sense for pure CPU-bound work. Because Cassandra request handling includes disk I/O and network waits, threads spend most of their time blocked. The fix changes the default to `availableProcessors() * 4`, providing enough parallelism to keep CPUs busy while I/O completes.

## Example
```java
// BEFORE
conf.rpc_max_threads = conf.rpc_server_type.equals("hsha")
    ? Runtime.getRuntime().availableProcessors()     // too few for I/O-bound work
    : Integer.MAX_VALUE;
```
```java
// AFTER
conf.rpc_max_threads = conf.rpc_server_type.equals("hsha")
    ? Runtime.getRuntime().availableProcessors() * 4 // enough for I/O wait
    : Integer.MAX_VALUE;
```
