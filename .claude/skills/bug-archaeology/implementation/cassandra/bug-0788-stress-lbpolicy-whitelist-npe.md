# Bug 0788: cassandra-stress LB policy ignores whitelist when no DC set and never wraps TokenAwarePolicy

**Commit**: 12c63cf4a4 · **Ticket**: CASSANDRA-12919
**Subsystem**: Tools / cassandra-stress
**Category**: Logic error in condition

---

## Root Cause
The original `JavaDriverClient` constructor built the `LoadBalancingPolicy` inline with two independent conditionals. First it would set `loadBalancingPolicy` to a `DCAwareRoundRobinPolicy` only when a datacenter was specified. Then it would wrap that in a `WhiteListPolicy` when `isWhiteList` was true — but if no DC was set and `isWhiteList` was true, it would call `new WhiteListPolicy(policyBuilder.build(), ...)` using a freshly-built (not yet-assigned) base policy while `loadBalancingPolicy` was still `null`. Additionally, no code path ever wrapped the result in a `TokenAwarePolicy`, so token-aware routing was never used even when it would significantly reduce cross-node hops during a stress run.

### Before / After
```java
// BEFORE — whitelist path silently uses a separate policyBuilder.build()
//           and TokenAwarePolicy is never applied
if (settings.node.isWhiteList)
    loadBalancingPolicy = new WhiteListPolicy(policyBuilder.build(), ...);
else if (settings.node.datacenter != null)
    loadBalancingPolicy = policyBuilder.build();
else
    loadBalancingPolicy = null;

// AFTER — factored into a helper; always wraps with TokenAwarePolicy;
//          WhiteListPolicy correctly uses the already-built DC-aware policy
private LoadBalancingPolicy loadBalancingPolicy(StressSettings settings) {
    DCAwareRoundRobinPolicy.Builder b = DCAwareRoundRobinPolicy.builder();
    if (settings.node.datacenter != null) b.withLocalDc(settings.node.datacenter);
    LoadBalancingPolicy ret = (settings.node.datacenter != null) ? b.build() : null;
    if (settings.node.isWhiteList)
        ret = new WhiteListPolicy(ret == null ? b.build() : ret, ...);
    return new TokenAwarePolicy(ret == null ? b.build() : ret);
}
```

---

## How to Spot
When building a chain of driver policies where each layer optionally wraps the previous one, use a single local variable updated in sequence rather than independent conditionals that can fall through to a `null` base. Also audit every policy-building path to confirm that all desired policy layers (e.g. `TokenAwarePolicy`) are always present in the chain regardless of option combinations.

---

## Test
No test added.
