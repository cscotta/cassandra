# Bug 2571: Repair streaming uses local interface address instead of broadcast address

**Commit**: f8be55b0bd · **Ticket**: CASSANDRA-3194

## Detection Rule
When a distributed task uses `FBUtilities.getLocalAddress()` to identify the current node in a forwarded message, nodes behind NAT or with separate broadcast addresses will send messages from the wrong address, causing the remote endpoint to reply to an unreachable address and the forwarding loop to break.

## Code Smell
`FBUtilities.getLocalAddress()` (binds address) appears in place of `FBUtilities.getBroadcastAddress()` (routable address) in any network message construction, node-identity assertion, or `equals()` comparison used to decide whether to initiate or forward a streaming repair task.

## Tags
- scope: cassandra
- category: wrong-constant
- code-pattern: local-vs-broadcast-address
- severity: silent-wrong-result
- subsystem: repair

## Root Cause
`StreamingRepairTask` used `FBUtilities.getLocalAddress()` at four call sites: to generate the task UUID, to decide whether this node is the source, and to assert task ownership in response handlers. When a node has a different broadcast address (e.g., EC2 public IP vs. private bind address), these comparisons always failed: the source check misdirected the task, and the response handler assertion fired, breaking the repair streaming forwarding loop. Replacing all four with `getBroadcastAddress()` ensures consistent identity.

## Example
```java
// BEFORE
InetAddress local = FBUtilities.getLocalAddress();
InetAddress src = ep2.equals(local) ? ep2 : ep1;
...
if (src.equals(FBUtilities.getLocalAddress()))
    initiateStreaming();
```
```java
// AFTER
InetAddress local = FBUtilities.getBroadcastAddress();
InetAddress src = ep2.equals(local) ? ep2 : ep1;
...
if (src.equals(FBUtilities.getBroadcastAddress()))
    initiateStreaming();
```
