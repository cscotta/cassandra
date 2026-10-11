**Commit**: e8459d3864 · **Ticket**: KAFKA-19875

**Subsystem**: clients/src/main/java/org/apache/kafka/common/config/ConfigDef.java

**Category**: Logic error in condition

**Root cause**
`ConfigDef` parsed `ValidList` configuration values by splitting on commas, but it performed no deduplication. A user who accidentally (or intentionally) specified the same value twice in a list-typed config (e.g. `listener.security.protocol.map`) would silently get a list containing duplicates, which could then cause downstream components that assume uniqueness to behave incorrectly. The fix adds a deduplication step for `ValidList` keys after parsing: if the size decreases after `distinct()`, a `WARN` log is emitted.

**Before**
```java
// Before: parsed list kept as-is, duplicates retained
parsedValue = parseType(key.name, value, key.type);
```

**After**
```java
// After: deduplicate with warning
if (key.validator instanceof ValidList && parsedValue instanceof List) {
    List<?> orig = (List<?>) parsedValue;
    parsedValue = orig.stream().distinct().collect(Collectors.toList());
    if (orig.size() != ((List<?>) parsedValue).size()) {
        LOGGER.warn("Duplicate values found in config '{}', deduplicating.", key.name);
    }
}
```

**How to spot**
In `ConfigDef.parseValue`, look for any `ValidList` processing that does not call `distinct()` on the resulting list; duplicate values in list-typed configs will silently be retained and may cause subtle misconfigurations downstream.

**Test**
Configure a `ValidList` key with a duplicated value, parse it through `ConfigDef`, and assert that the returned list contains only one copy and that a warning was logged.
