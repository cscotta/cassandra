**Commit**: c00d1c7cb8 · **Ticket**: KAFKA-20158
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Adds support for `RocksDBSessionStore` to support headers for

[KIP-1271](https://cwiki.apache.org/confluence/display/KAFKA/KIP-1271%3A+Allow+to+Store+Record+Headers+in+State+Stores)
and includes testing for lazily converting the format in the open in
upgrade mode.

**Before/After**
```java
// Before
        return new RocksDbSessionBytesStoreSupplier(name, retentionPeriodMs);

// After
        return persistentSessionStore(name, retentionPeriod, false);
    }

    /**
     * Create a persistent {@link SessionBytesStoreSupplier} with support for record headers.
     * <p>
     * Note that it is not safe to change the value of {@code retentionPeriod} between
     * application restarts without clearing local state from application instances,
     * as this may cause incorrect values to be read from the state store if it impacts
     * the underlying storage format.
     *
     * @param name              name of the store (cannot be {@code null})
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20158 rocksdb session stores with headers support (4/n) (#21571).

**Test**
Test modified in AbstractSessionBytesStoreTest.java covering the corrected behaviour.
