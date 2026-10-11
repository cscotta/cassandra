## Bug 510 — ShareGroupOffset: deliveryCompleteCount initialised to -1 instead of 0

**Commit:** 3e9ae03b9c
**Ticket:** (share-group fix — no standalone JIRA cited)
**Subsystem:** server / share-group coordinator
**Category:** Wrong constant/default value

### Root cause
`PartitionFactory.java` initialised a new `ShareGroupOffset` with `deliveryCompleteCount = UNINITIALIZED_DELIVERY_COMPLETE_COUNT` (the sentinel value `-1`) even when `startOffset >= 0`, meaning the partition had a known starting position. Consumers reading this field would treat a valid newly-created share-group offset as uninitialised, triggering fallback code paths or incorrect delivery-count reporting.

### Before
```java
new ShareGroupOffset(startOffset, UNINITIALIZED_DELIVERY_COMPLETE_COUNT, …)
// UNINITIALIZED_DELIVERY_COMPLETE_COUNT = -1
```

### After
```java
new ShareGroupOffset(startOffset, 0, …)
// 0 is the correct starting count for a fresh partition
```

### How to spot
Initialiser calls that use a sentinel constant (negative value) in a field that should hold a real zero-based count are a sign that "no value" and "value is zero" have been conflated. Verify all construction sites when a sentinel is defined alongside a real zero default.

### Test
`ShareGroupOffsetTest` and `PartitionFactoryTest` updated to assert `deliveryCompleteCount == 0` for newly created offsets.
