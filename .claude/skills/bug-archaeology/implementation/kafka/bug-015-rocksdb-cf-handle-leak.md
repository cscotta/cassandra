**Commit**: 5e509dc364 · **Ticket**: KAFKA-20287
**Subsystem**: streams — RocksDBStore / RocksDB column-family handle management
**Category**: Resource leak

**Root cause**
Multiple `openRocksDB`-related methods failed to close RocksDB column-family (CF) handles when an exception occurred partway through initialization. Specifically: (1) in `openRocksDB()`, if `createColumnFamilies()` or `mergeColumnFamilyHandleLists()` threw after `RocksDB.open()` succeeded, the existing CF handles were not closed; (2) in `openDB()`, if `openRocksDB()` succeeded but `cfAccessor.open()` threw, no native resources were cleaned up; (3) in migration stores (`RocksDBMigratingSessionStoreWithHeaders`, `RocksDBTimestampedStoreWithHeaders`), the `RocksIterator` was manually closed after the try-block, missing the close if an exception occurred. CF handles are native objects; leaking them causes memory/file-descriptor exhaustion.

**Before/After**
```java
// Before — no cleanup on partial failure
db = RocksDB.open(..., existingDescriptors, existingColumnFamilies);
final List<ColumnFamilyHandle> created = db.createColumnFamilies(toCreate);
return mergeColumnFamilyHandleLists(...);  // if this throws, handles leaked

// After — finally block closes handles on any exception
try {
    db = RocksDB.open(...);
    createdColumnFamilies.addAll(db.createColumnFamilies(toCreate));
    return mergeColumnFamilyHandleLists(...);
} catch (Exception e) {
    for (ColumnFamilyHandle h : existingColumnFamilies) h.close();
    for (ColumnFamilyHandle h : createdColumnFamilies)  h.close();
    throw e;
}
```

**How to spot**
Any code that calls `RocksDB.open()` followed by additional operations (create CF, merge handles, open accessor) without a `finally` or try-with-resources block is a CF handle leak. Also, `RocksIterator` is `AutoCloseable` and must always be used in a try-with-resources block. Review all `openDB` / `openRocksDB` overrides in RocksDB-based stores for missing cleanup paths.

**Test**
No dedicated unit test for the resource-leak scenarios was added; the fix is structural (finally blocks). Existing RocksDB store tests exercise the open paths and would catch regressions.
