**Commit**: feba1af46b · **Ticket**: KAFKA-19713
**Subsystem**: streams/src
**Category**: Resource leak

**Root cause**
Follow-up PR for KAFKA-19713 that includes test cases for:

- Persist Position offsets in the `offsets` ColumnFamily
- Position gets correctly merged across all segments in Segmented stores
- Persist position after every commit
- Restore positions after store close->open

This PR also refactors AbstractColumnFamily tests to not use a mock by
creating an in-memory version of a RocksDBAccessor. Reviewers: Bill Bejeck <bbejeck@apache.org>

---------

Co-authored-by: Bill Bejeck <bbejeck@gmail.com>

**Before/After**
```java
// Before
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
        when(dbAccessor.get(offsetsCF, toBytes("status"))).thenReturn(closedValue);

        verify(dbAccessor).put(eq(offsetsCF), eq(toBytes("status")), eq(openValue));
        verify(dbAccessor).put(eq(offsetsCF), eq(toBytes("status")), eq(closedValue));
        when(dbAccessor.get(offsetsCF, toBytes("status"))).thenReturn(openValue);
        when(dbAccessor.get(offsetsCF, toBytes("status"))).thenReturn(openValue);
        verify(dbAccessor).put(eq(offsetsCF), eq(toBytes("status")), eq(openValue));
        verify(dbAccessor).flush(any(ColumnFamilyHandle[].class));

// After
import org.apache.kafka.streams.query.Position;
import org.rocksdb.RocksDB;
import java.nio.ByteBuffer;
import static org.apache.kafka.common.utils.Utils.mkEntry;
import static org.apache.kafka.common.utils.Utils.mkMap;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
        dbAccessor = new InMemoryRocksDBAccessor(mock(RocksDB.class));
        assertArrayEquals(openValue, dbAccessor.get(offsetsCF, toBytes("status")));
        assertArrayEquals(closedValue, dbAccessor.get(offsetsCF, toBytes("status")));

```

**How to spot**
When reviewing streams/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: minor: test coverage for segmented stores (#21860).

**Test**
Test modified in AbstractColumnFamilyAccessorTest.java covering the corrected behaviour.
