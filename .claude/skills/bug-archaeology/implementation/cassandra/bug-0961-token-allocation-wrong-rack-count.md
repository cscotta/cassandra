# Bug 0961: Token Allocation Wrong Rack Count and Inverted Condition (Backport)

**Commit**: 2954b18354 · **Ticket**: CASSANDRA-11139
**Subsystem**: Token allocation / bootstrapping
**Category**: Logic error in condition

---

## Root Cause
This is the same fix as bug-0960 applied to a different branch. `TokenAllocation.getOrCreateAllocator` called `.size()` on a `Multimap` value (returning total entries, not unique racks) and had its guard condition inverted (`replicas >= racks` instead of `racks >= replicas`), causing the wrong rack-aware strategy adapter to be selected during vnode allocation.

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
