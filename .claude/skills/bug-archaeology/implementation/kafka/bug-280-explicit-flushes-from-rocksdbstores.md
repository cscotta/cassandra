**Commit**: 4becf48158 · **Ticket**: KAFKA-19712
**Subsystem**: streams/src
**Category**: Race condition / missing synchronization

**Root cause**
RocksDBStores now manage their own offsets by storing them in a
`offsets` ColumnFamily. Also, these stores use a RocksDB atomic flush to
guarantee consistency between the default and offsets ColumnFamilies. As
described in KIP-1035, this new behavior enables RocksDB stores to avoid
explicitly flushing the memtables to the SST files. This is a follow-up PR for https://github.com/apache/kafka/pull/21578 we
can now implement this because KAFKA-19712 is solved.

**Before/After**
```java
// Before
        // We need to remove this flush call when implementing KAFKA-19712
        this.flush(accessor, offsetColumnFamilyHandle);
    /**
     * Invokes commit in the underlying ColumnFamilyAccessor.
     * Subclasses should implement this method to define specific commit behavior.
     * This method will be removed when implementing KAFKA-19712
     *
     * @param accessor the RocksDB accessor used to interact with the database
     * @throws RocksDBException if an error occurs during the commit operation
     */
    protected abstract void flush(final RocksDBStore.DBAccessor accessor, final ColumnFamilyHandle offsetColumnFamilyHandle) throws RocksDBException;
    @Override

// After
// (no additions)
```

**How to spot**
When reviewing streams/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor:remove explicit flushes from rocksdbstores (#21871).

**Test**
Test modified in DualColumnFamilyAccessorTest.java covering the corrected behaviour.
