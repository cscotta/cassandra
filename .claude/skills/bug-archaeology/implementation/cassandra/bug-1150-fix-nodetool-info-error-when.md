# Bug 1150: nodetool info Crashes When Node Has Not Yet Joined the Cluster

**Commit**: 20f12e9744 · **Ticket**: CASSANDRA-9031
**Subsystem**: src/java/org/apache/cassandra/tools/NodeTool.java
**Category**: Missing null / bounds check

---

## Root Cause

`nodetool info` unconditionally called `probe.getTokens()` to print token information. When the node has not yet joined the ring it has no tokens assigned, and the legacy `getEndpoint()` implementation looked up the node's endpoint by matching its first token against the token-to-endpoint map—a map that is empty before joining. This caused an uncaught `RuntimeException` ("Could not find myself in the endpoint list") that terminated the `info` command with an unhelpful error. The fix guards the token-printing block behind `probe.isJoined()` and replaces the fragile token-based endpoint lookup with a direct host-ID lookup, which is available even before the node joins.

### Before / After

```java
// BEFORE – unconditional token access throws when node is not joined
List<String> tokens = probe.getTokens();
if (tokens.size() == 1 || this.tokens)
    for (String token : tokens)
        System.out.printf("%-23s: %s%n", "Token", token);
else
    System.out.printf("%-23s: (invoke with -T/--tokens ...)%n", "Token", tokens.size());
```

```java
// AFTER – gate on isJoined(); print a safe message when not yet joined
if (probe.isJoined())
{
    List<String> tokens = probe.getTokens();
    if (tokens.size() == 1 || this.tokens)
        for (String token : tokens)
            System.out.printf("%-23s: %s%n", "Token", token);
    else
        System.out.printf("%-23s: (invoke with -T/--tokens ...)%n", "Token", tokens.size());
}
else
{
    System.out.printf("%-23s: (node is not joined to the cluster)%n", "Token");
}
```

---

## How to Spot

Any diagnostic tool that queries ring-state (tokens, endpoints, ranges) should be gated on a joined/initialized check before calling those APIs. A reviewer should ask: "what happens if this node is in bootstrap, decommissioning, or stopped mid-join?" and verify each state-dependent call is guarded accordingly.

---

## Test

No test added.
