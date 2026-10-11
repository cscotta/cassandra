**Commit**: 97cdaaa15f · **Ticket**: KAFKA-17312
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
            add("maxBackgroundCompactions");
            add("setMaxBackgroundCompactions");
            add("maxBackgroundFlushes");
            add("setMaxBackgroundFlushes");
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17312 fix tests shouldforwardalldboptionscalls and shouldoverwritealloptionsmethods in rocksdbgenericoptionstodbop.

**Test**
Test modified in RocksDBGenericOptionsToDbOptionsColumnFamilyOptionsAdapterTest.java covering the corrected behaviour.
