**Commit**: 2eacc1cad4 · **Ticket**: KAFKA-20260
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
Extends headers-aware key-value store migration to support upgrading
from plain (non-timestamped) stores. - Plain-to-Headers Migration Support
- RocksDB Upgrade Path
- Implemented `query()` method in the adapter class

Testing:
- 3 upgrade intergration tests
- 2 downgrade integration tests
- unit tests for all added methods/classes

Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            Stores.timestampedKeyValueStoreBuilder(
                persistentStore ? Stores.persistentTimestampedKeyValueStore(STORE_NAME) : Stores.inMemoryKeyValueStore(STORE_NAME),
                Serdes.String(),
                Serdes.String()))
            Stores.timestampedKeyValueStoreBuilderWithHeaders(
                persistentStore ? Stores.persistentTimestampedKeyValueStoreWithHeaders(STORE_NAME) : Stores.inMemoryKeyValueStore(STORE_NAME),
                Serdes.String(),
                Serdes.String()))
            Stores.timestampedKeyValueStoreBuilder(
                Stores.persistentTimestampedKeyValueStore(STORE_NAME),
                Serdes.String(),
                Serdes.String()))

// After
import org.apache.kafka.streams.state.KeyValueStore;
                Stores.timestampedKeyValueStoreBuilder(
                    persistentStore ? Stores.persistentTimestampedKeyValueStore(STORE_NAME) : Stores.inMemoryKeyValueStore(STORE_NAME),
                    Serdes.String(),
                    Serdes.String()))
                Stores.timestampedKeyValueStoreBuilderWithHeaders(
                    persistentStore ? Stores.persistentTimestampedKeyValueStoreWithHeaders(STORE_NAME) : Stores.inMemoryKeyValueStore(STORE_NAME),
                    Serdes.String(),
                    Serdes.String()))
                Stores.timestampedKeyValueStoreBuilder(
                    Stores.persistentTimestampedKeyValueStore(STORE_NAME),
                    Serdes.String(),
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20260: upgrade path from plain kv store to headers store (#21666).

**Test**
Test modified in HeadersStoreUpgradeIntegrationTest.java covering the corrected behaviour.
