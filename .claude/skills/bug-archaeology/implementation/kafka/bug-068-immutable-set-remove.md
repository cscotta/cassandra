**Commit**: c7caf912aa · **Ticket**: KAFKA-19524
**Subsystem**: tools/src/main/java/org/apache/kafka/tools/ConnectPluginPath.java
**Category**: Logic error in condition

**Root cause**
`ConnectPluginPath` built a `nonLoadableManifests` map and, for each loadable plugin descriptor, attempted to remove the matching type from the map entry's `Set` with:
```java
nonLoadableManifests.getOrDefault(pluginDesc.className(), Collections.emptySet()).remove(pluginDesc.type());
```
`Collections.emptySet()` returns an immutable set. When the key was absent from the map, `getOrDefault` returned the immutable empty set and `remove()` threw `UnsupportedOperationException`. The fix changes to a null-check pattern that only calls `remove()` on sets that actually come from the map.

**Before/After**
```java
// Before: remove() on immutable Collections.emptySet() throws UnsupportedOperationException
nonLoadableManifests.getOrDefault(pluginDesc.className(),
    Collections.emptySet()).remove(pluginDesc.type());

// After: only mutate if the key exists in the map
var types = nonLoadableManifests.get(pluginDesc.className());
if (types != null) types.remove(pluginDesc.type());
```

**How to spot**
Any call to `Map.getOrDefault(key, Collections.emptyXxx())` followed by a mutating method is incorrect. `Collections.emptySet/emptyList/emptyMap` return immutable views. Either use `computeIfAbsent` with a mutable collection supplier, or use `get` and check for null before mutating.

**Test**
No new test was added; the fix is exercised by running `connect-plugin-path` against plugins that have loadable classes but no ServiceLoader manifests, which previously triggered the `UnsupportedOperationException`.
