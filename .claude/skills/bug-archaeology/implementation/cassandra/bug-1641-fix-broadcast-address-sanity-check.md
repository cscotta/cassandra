# Bug 1641: Fix Broadcast Address Sanity Check

**Commit**: 65cd825d0d · **Ticket**: n/a
**Subsystem**: src/java/org/apache/cassandra/locator (YamlFileNetworkTopologySnitch)
**Category**: Logic error in condition

---

## Root Cause

In `YamlFileNetworkTopologySnitch`, when iterating over nodes within a rack to validate configuration, the null check guarded the wrong field: it tested `rack.rack_name == null` instead of `node.broadcast_address == null`. A rack with a missing name would never trigger the error regardless of its nodes' broadcast addresses, while a node with a missing broadcast address would silently pass validation and cause a NullPointerException or misconfiguration later.

### Before / After

```java
// BEFORE
for (final Node node : rack.nodes)
{
    if (rack.rack_name == null)
    {
        throw new ConfigurationException(...);
    }
```

```java
// AFTER
for (final Node node : rack.nodes)
{
    if (node.broadcast_address == null)
    {
        throw new ConfigurationException(...);
    }
```

---

## How to Spot

In loops that iterate over nested objects, verify that each null/bounds check references a field on the loop variable, not a field on the outer container. A check on the outer object inside a loop body will be evaluated identically for every iteration, which is almost never correct.

---

## Test

No test added.
