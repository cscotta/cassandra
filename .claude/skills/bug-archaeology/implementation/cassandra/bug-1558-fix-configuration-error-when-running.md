# Bug 1558: Fix Configuration Error When Running nodetool ring

**Commit**: 721afaead6 · **Ticket**: CASSANDRA-7508
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Wrong constant / default value

---

## Root Cause

The `nodetool ring` command printed a vnode warning by calling `DatabaseDescriptor.getNumTokens() > 1`, which reads the local node's configuration file. When `nodetool` is run as a remote tool against a node it does not co-locate with, `DatabaseDescriptor` may not be initialized, causing a configuration error. More fundamentally, the local config value is not authoritative — a remote node may be configured differently. The fix derives the vnode status directly from the live ring data: if any endpoint appears more than once in the token-to-endpoint map, the cluster is using vnodes.

### Before / After

```java
// BEFORE
if (DatabaseDescriptor.getNumTokens() > 1)
{
    System.out.println("  Warning: \"nodetool ring\" is used to output all the tokens...");
```

```java
// AFTER
boolean haveVnodes = false;
for (Map.Entry<String, String> entry : tokensToEndpoints.entrySet())
{
    haveVnodes |= endpointsToTokens.containsKey(entry.getValue());
    endpointsToTokens.put(entry.getValue(), entry.getKey());
}
// ...
if (haveVnodes)
{
    System.out.println("  Warning: \"nodetool ring\" is used to output all the tokens...");
```

---

## How to Spot

CLI tools that operate remotely via JMX should never read `DatabaseDescriptor` for state that describes the remote node — it reflects only the local configuration. Always derive node properties from the JMX/probe data returned by the target node.

---

## Test

No test added.
