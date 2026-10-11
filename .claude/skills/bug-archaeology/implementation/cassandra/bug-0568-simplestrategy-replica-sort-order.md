# Bug 0568: SimpleStrategy Sorts Replicas by DC, Breaking Primary Range Assignment

**Commit**: 2adfa92044 · **Ticket**: CASSANDRA-14862
**Subsystem**: `src/java/org/apache/cassandra/locator/SimpleStrategy.java`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`SimpleStrategy.calculateNaturalReplicas` was sorting the resulting replica list by datacenter using the endpoint snitch after the natural ring-order replicas had already been assembled. The contract of `calculateNaturalReplicas` — documented by the parent class — is that replicas are returned in ring order, since the first replica in the list is implicitly the "primary" replica for that range. Sorting by DC disrupted that ordering, causing incorrect primary range assignments that affected repair and size estimate calculations in multi-DC clusters using SimpleStrategy.

### Before / After

```java
// BEFORE
IEndpointSnitch snitch = DatabaseDescriptor.getEndpointSnitch();
return replicas.build()
               .sorted(Comparator.comparing(r -> snitch.getDatacenter(r.endpoint())));
```

```java
// AFTER
return replicas.build();
```

---

## How to Spot

When a method assembles an ordered collection (e.g., ring-walk replicas) and then applies a secondary sort, check whether any caller depends on the original ordering. Ring-order replica lists are commonly used to determine "primary" ownership; any sort applied after construction that ignores the ring position will silently corrupt downstream range-ownership logic. Look for `Comparator`-based sorts on replica or endpoint lists returned from replication-strategy methods.

---

## Test

The new `testMultiDCSimpleStrategyEndpoints` test constructs a 3-node, 2-DC topology with RF=3 and verifies that each node holds exactly 2 primary ranges and 6 total replica ranges, catching the case where DC-based sorting would shift primary ownership unevenly.
