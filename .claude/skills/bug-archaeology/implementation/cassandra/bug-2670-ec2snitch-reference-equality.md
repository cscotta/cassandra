# Bug 2670: Identity comparison instead of equals() for InetAddress in Ec2Snitch

**Commit**: 5703b6b056 · **Ticket**: CASSANDRA-2733

## Detection Rule
When comparing `InetAddress` objects (or any non-primitive objects) for logical equality, use `.equals()` rather than the `==` reference-equality operator.

## Code Smell
`if (endpoint == FBUtilities.getLocalAddress())` — reference comparison of two separately-created `InetAddress` instances that may represent the same address but are different objects.

## Tags
- scope: java
- category: logic-error
- code-pattern: reference-equality-object
- severity: silent-wrong-result
- subsystem: gossip

## Root Cause
`Ec2Snitch.getRack()` and `getDatacenter()` used `==` to compare the queried `endpoint` with the local address. When the objects are not the same reference (e.g., deserialized from gossip), the comparison always returns `false`, causing the snitch to try to look up the local node's rack/DC from gossip state instead of the locally-known values. This breaks `nodetool ring` and any topology-sensitive operation for the local node.

## Example
```java
// BEFORE
public String getRack(InetAddress endpoint)
{
    if (endpoint == FBUtilities.getLocalAddress())
        return ec2zone;
    else
        return Gossiper.instance.getEndpointStateForEndpoint(endpoint)
                       .getApplicationState(ApplicationState.RACK).value;
}
```
```java
// AFTER
public String getRack(InetAddress endpoint)
{
    if (endpoint.equals(FBUtilities.getLocalAddress()))
        return ec2zone;
    return Gossiper.instance.getEndpointStateForEndpoint(endpoint)
                   .getApplicationState(ApplicationState.RACK).value;
}
```
