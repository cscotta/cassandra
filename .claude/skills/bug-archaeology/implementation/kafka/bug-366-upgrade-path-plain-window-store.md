**Commit**: 8cad040374 · **Ticket**: KAFKA-20261
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Extends headers-aware window store migration to support upgrading from
plain (non-timestamped) stores. - Plain-to-Headers Migration Support
- Implemented query() method in the adapter class

Testing:
- 3 upgrade intergration tests
- 2 downgrade integration tests

Reviewers: Bill Bejeck <bill@confluent.io>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
// (no removals)

// After
import org.apache.kafka.streams.state.WindowStore;
    @Test
    public void shouldMigrateInMemoryPlainWindowStoreToTimestampedWindowStoreWithHeaders() throws Exception {
        shouldMigratePlainWindowStoreToTimestampedWindowStoreWithHeaders(false);
    }

    @Test
    public void shouldMigratePersistentPlainWindowStoreToTimestampedWindowStoreWithHeaders() throws Exception {
        shouldMigratePlainWindowStoreToTimestampedWindowStoreWithHeaders(true);
    }

    private void shouldMigratePlainWindowStoreToTimestampedWindowStoreWithHeaders(final boolean persistentStore) throws Exception {
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20261: upgrade path for plain window store to headers store (#21710).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
