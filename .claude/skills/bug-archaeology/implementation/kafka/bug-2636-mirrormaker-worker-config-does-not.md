**Commit**: 8d8c367066 · **Ticket**: KAFKA-17192
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chris Egerton <chrise@aiven.io>

**Before/After**
```java
// Before
            if (entry.getKey().startsWith(prefix) && entry.getKey().length() > prefix.length()) {
        return Utils.entriesWithPrefix(rawProperties, prefix, false);

// After
        return entriesWithPrefix(map, prefix, strip, false);
    }

    /**
     * Find all key/value pairs whose keys begin with the given prefix, optionally removing that prefix
     * from all resulting keys.
     * @param map the map to filter key/value pairs from
     * @param prefix the prefix to search keys for
     * @param strip whether the keys of the returned map should not include the prefix
     * @param allowMatchingLength whether to include keys that are exactly the same length as the prefix
     * @return a {@link Map} containing a key/value pair for every key/value pair in the {@code map}
     * parameter whose key begins with the given {@code prefix}; may be empty, but never null
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17192 fix mirrormaker2 worker config does not pass config.provi… (#16678).

**Test**
Test modified in MirrorMakerConfigTest.java covering the corrected behaviour.
