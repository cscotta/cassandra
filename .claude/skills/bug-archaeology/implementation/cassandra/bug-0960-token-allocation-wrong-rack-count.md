# Bug 0960: Token Allocation Wrong Rack Count and Inverted Condition

**Commit**: 9b0adf2294 · **Ticket**: CASSANDRA-11139
**Subsystem**: Token allocation / bootstrapping
**Category**: Logic error in condition

---

## Root Cause
`TokenAllocation.getOrCreateAllocator` called `.size()` on the `Multimap` returned by `topology.getDatacenterRacks().get(dc)` directly, which counts total entries (endpoint-to-rack mappings) rather than unique racks — the correct call is `.asMap().size()`. Compounding the error, the guard condition comparing replicas to racks was inverted: the code used `replicas >= racks` where it should be `racks >= replicas`, completely reversing the strategy selection logic.

### Before / After
```java
// BEFORE
int racks = topology.getDatacenterRacks().get(dc).size();  // counts total entries, not unique racks

if (replicas >= racks)   // condition is backwards
{
    return new StrategyAdapter() { ... }

// AFTER
int racks = topology.getDatacenterRacks().get(dc).asMap().size(); // unique racks

if (racks >= replicas)   // correct: choose rack-aware strategy when there are enough racks
{
    return new StrategyAdapter() { ... }
```

---

## How to Spot
Whenever a `Multimap` is used to represent a one-to-many relationship (e.g., rack → endpoints), calling `.size()` on the multimap value (which is itself a collection) gives the number of all values, not the number of distinct keys. Look for `.size()` calls on `Multimap` or `Multimap.get()` return values where the intent is "how many distinct groups exist" — those calls should use `.asMap().size()` instead. Additionally, boolean guard conditions of the form `X >= Y` near topology branching deserve careful scrutiny to confirm the comparison direction matches the natural-language intent.

---

## Test
`BootStrapperTest` was extended with a multi-rack, multi-DC scenario that verifies the correct allocator variant is selected and that token placement properly accounts for rack count.
