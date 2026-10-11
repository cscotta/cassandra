**Commit**: fc4b739578 · **Ticket**: KAFKA-17854
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
            StreamsTestUtils.toList(windowStore.fetch("a", null, ofEpochMilli(0), ofEpochMilli(10)));
            KeyValue.pair(new Windowed<>("a", new TimeWindow(0, WINDOW_SIZE)), "a"),
            StreamsTestUtils.toList(windowStore.fetch(null, "c", ofEpochMilli(0), ofEpochMilli(10)));
            KeyValue.pair(new Windowed<>("b", new TimeWindow(10, 10 + WINDOW_SIZE)), "b"),
            KeyValue.pair(new Windowed<>("c", new TimeWindow(10, 10 + WINDOW_SIZE)), "c"))));
            StreamsTestUtils.toList(windowStore.backwardFetch("a", null, ofEpochMilli(0), ofEpochMilli(10)));
            KeyValue.pair(new Windowed<>("a", new TimeWindow(0, WINDOW_SIZE)), "a"),
            StreamsTestUtils.toList(windowStore.backwardFetch(null, "c", ofEpochMilli(0), ofEpochMilli(10)));
            KeyValue.pair(new Windowed<>("c", new TimeWindow(10, 10 + WINDOW_SIZE)), "c"),

// After
            StreamsTestUtils.toList(windowStore.fetch("b", null, ofEpochMilli(0), ofEpochMilli(10)));
            StreamsTestUtils.toList(windowStore.fetch(null, "b", ofEpochMilli(0), ofEpochMilli(10)));
            KeyValue.pair(new Windowed<>("b", new TimeWindow(10, 10 + WINDOW_SIZE)), "b"))));
            StreamsTestUtils.toList(windowStore.backwardFetch("b", null, ofEpochMilli(0), ofEpochMilli(10)));
            StreamsTestUtils.toList(windowStore.backwardFetch(null, "b", ofEpochMilli(0), ofEpochMilli(10)));
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17854 improve tests for readonlywindowstorestub#fetch and #backwardfetch (#17586).

**Test**
Test modified in CompositeReadOnlyWindowStoreTest.java covering the corrected behaviour.
