# Bug 0449: StreamMessage Type Lookup Returns Null for Valid IDs

**Commit**: 2958c55870 · **Ticket**: CASSANDRA-16131
**Subsystem**: `src/java/org/apache/cassandra/streaming/messages/StreamMessage.java`
**Category**: Missing null / bounds check

---

## Root Cause

The `StreamMessage.Type` enum built its reverse-lookup table as a plain array (`Type[] idToTypeMap`) sized to `max(id) + 1`. The enum's message IDs are not zero-based contiguous integers — ID `0` is not assigned to any type. So `idToTypeMap[0]` was always `null`. Any caller that received a stream message with id=0 (or any other gap ID) would get a `null` return rather than an `IllegalArgumentException`, leading to a downstream `NullPointerException` instead of a clear error. The fix replaces the array with a `HashMap<Integer, Type>` and changes the `lookupById` guard to check for a `null` map result rather than an out-of-range index.

### Before / After

```java
// BEFORE
private static final Type[] idToTypeMap;
static {
    Type[] idMap = new Type[max + 1];
    for (Type t : values())
        idMap[t.id] = t;  // gaps (e.g. id 0) remain null
    idToTypeMap = idMap;
}
public static Type lookupById(int id) {
    if (id < 0 || id >= idToTypeMap.length)
        throw new IllegalArgumentException("Invalid type id: " + id);
    return idToTypeMap[id];  // may return null silently
}
```

```java
// AFTER
private static final Map<Integer, Type> idToTypeMap;
static {
    idToTypeMap = new HashMap<>();
    for (Type t : values())
        if (idToTypeMap.put(t.id, t) != null)
            throw new RuntimeException("Two StreamMessage Types map to the same id: " + t.id);
}
public static Type lookupById(int id) {
    Type t = idToTypeMap.get(id);
    if (t == null)
        throw new IllegalArgumentException("Invalid type id: " + id);
    return t;
}
```

---

## How to Spot

Array-backed enum reverse-lookup tables are safe only when enum ordinals are dense and zero-based. Whenever enum constants carry explicit numeric IDs (especially protocol codes that may start at 1 or skip values), a gap in the ID space leaves array slots `null` and converts an invalid-input error into a silent `null` return. Reviewers should verify that bounds checking also covers the interior of the array, not just the endpoints.

---

## Test

`StreamMessageTest.testTypeLookup` verifies that every assigned id returns a non-null `Type`, and that a set of boundary/gap ids (including 0, -1, `max+1`, `Integer.MAX_VALUE`, `Integer.MIN_VALUE`) all throw `IllegalArgumentException`.
