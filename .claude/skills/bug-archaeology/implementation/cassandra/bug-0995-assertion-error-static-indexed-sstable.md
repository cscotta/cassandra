# Bug 0995: Assertion Error Reading Static Columns on Indexed SSTable

**Commit**: 8bc567b3ca · **Ticket**: CASSANDRA-10903
**Subsystem**: SSTable I/O / column iterator
**Category**: Logic error in condition

---

## Root Cause
`AbstractSSTableIterator` passed an `isAtPartitionStart` boolean to `createReader`, which was used to initialize an `isInit` flag in the `Reader` base class. The flag's purpose was to defer a call to `init()` until the first `hasNext()` invocation, because in certain code paths (specifically indexed SSTables that hadn't yet seeked to a slice) the file position was not yet valid. When a table had a static column, the iterator read the static row before constructing the reader and then passed `isAtPartitionStart=true` even for indexed SSTables—the static row read had already advanced the file pointer past the partition header, so the reader assumed it was positioned at the beginning of the clustered row data. For indexed SSTables this caused `IndexState.updateBlock()` to hit `assert currentIndexIdx >= 0` because `setToBlock(0)` had never been called. The fix eliminates the `isInit`/`init()` machinery entirely and instead makes `updateBlock()` handle the `currentIndexIdx < 0` case by calling `setToBlock(0)` itself, removing the incorrect assumption.

### Before / After
```java
// BEFORE — isInit flag and deferred init() call
protected boolean isInit;
protected Reader(FileDataInput file, boolean isInit, boolean shouldCloseFile) { ... }
public boolean hasNext() {
    if (!isInit) { init(); isInit = true; }
    return hasNextInternal();
}
// updateBlock asserted the index was already valid
public void updateBlock() throws IOException {
    assert currentIndexIdx >= 0;  // would fire if static row was read before setToBlock(0)
    ...
}

// AFTER — no isInit, updateBlock self-initializes
protected Reader(FileDataInput file, boolean shouldCloseFile) { ... }
public void updateBlock() throws IOException {
    assert !reversed;
    if (currentIndexIdx < 0) {
        setToBlock(0);  // self-initialize on first entry
        return;
    }
    ...
}
```

---

## How to Spot
Two-phase initialization patterns (an `isInit` flag + deferred `init()` call) are fragile when the conditions that make initialization safe change between the flag-set point and the actual first use. When reading static columns changes the file position before a reader object is even created, any assumption that the reader will see a "clean" partition start becomes invalid. Look for `assert` statements guarding index or position state in iterator internals—they often reveal that the code assumes an initialization sequence that not all callers follow.

---

## Test
`QueryWithIndexedSSTableTest.queryIndexedSSTableTest` creates a table with a static column and a partition large enough to be indexed (1000 rows × 100-byte values), then queries `SELECT s` both forward and reverse, including `DISTINCT`, to exercise both the forward and reverse indexed reader paths.
