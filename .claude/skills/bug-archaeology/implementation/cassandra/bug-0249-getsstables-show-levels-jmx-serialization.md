# Bug 0249: `getsstables --show-levels` Fails with JMX Serialization Error

**Commit**: e936b2cc1b · **Ticket**: CASSANDRA-18140
**Subsystem**: `db/ColumnFamilyStore.java`, `tools/nodetool/GetSSTables.java`
**Category**: Wrong serialization / deserialization

---

## Root Cause

`ColumnFamilyStore.getSSTablesForKeyWithLevel()` returned `Map<Integer, Collection<String>>` where the `Collection` was the internal view of a Guava `HashMultimap` (i.e., a `Multimap.asMap()` value). When this type was serialized over JMX the RMI stub could not deserialize it on the client side because `HashMultimap`'s internal collection type is not a standard `java.util` type. The fix replaces the `Multimap` with a plain `HashMap<Integer, Set<String>>` whose values are `HashSet` instances, making the return type fully JMX-serializable.

### Before / After

```java
// BEFORE
public Map<Integer, Collection<String>> getSSTablesForKeyWithLevel(String key, boolean hexFormat)
{
    Multimap<Integer, String> result = HashMultimap.create();
    for (Pair<Integer, String> sst : ssts)
        result.put(sst.left, sst.right);
    return result.asMap();
}
```

```java
// AFTER
public Map<Integer, Set<String>> getSSTablesForKeyWithLevel(String key, boolean hexFormat)
{
    HashMap<Integer, Set<String>> result = new HashMap<>();
    for (Pair<Integer, String> sst : ssts) {
        Set<String> perLevel = result.computeIfAbsent(sst.left, k -> new HashSet<>());
        perLevel.add(sst.right);
    }
    return result;
}
```

---

## How to Spot

Any method exposed via a JMX MBean interface must return only types that are natively serializable by the JMX/RMI layer: standard `java.util` collections and primitive wrappers. Guava's `Multimap`, its `asMap()` views, and other non-`java.io.Serializable` collection wrappers silently fail during remote deserialization. During code review, verify that all MBean method signatures use only `java.util` types.

---

## Test

No dedicated test added; the fix is validated by running `nodetool getsstables --show-levels` against a node with leveled compaction.
