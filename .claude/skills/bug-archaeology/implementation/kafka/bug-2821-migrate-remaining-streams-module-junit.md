**Commit**: 26b9163487 · **Ticket**: KAFKA-15623
**Subsystem**: checkstyle/suppressions.xml
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              files="(EosV2UpgradeIntegrationTest|KStreamKStreamJoinTest|KTableKTableForeignKeyJoinIntegrationTest|KTableKTableForeignKeyVersionedJoinIntegrationTest|RocksDBGenericOptionsToDbOptionsColumnFamilyOptionsAdapterTest|RelationalSmokeTest|MockProcessorContextStateStoreTest|IQv2StoreIntegrationTest).java"/>
              files="(EosV2UpgradeIntegrationTest|EosTestDriver|KStreamKStreamJoinTest|KTableKTableForeignKeyJoinIntegrationTest|KTableKTableForeignKeyVersionedJoinIntegrationTest|RelationalSmokeTest|MockProcessorContextStateStoreTest|TopologyTestDriverTest|IQv2StoreIntegrationTest).java"/>
              files="(KStreamSlidingWindowAggregateTest|KStreamKStreamLeftJoinTest|KStreamKStreamOuterJoinTest|KTableKTableForeignKeyVersionedJoinIntegrationTest).java"/>
import java.util.Collections;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.test.IntegrationTest;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

// After
              files="(EosV2UpgradeIntegrationTest|KStreamKStreamJoinTest|KTableKTableForeignKeyJoinIntegrationTest|RocksDBGenericOptionsToDbOptionsColumnFamilyOptionsAdapterTest|RelationalSmokeTest|MockProcessorContextStateStoreTest|IQv2StoreIntegrationTest).java"/>
              files="(EosV2UpgradeIntegrationTest|EosTestDriver|KStreamKStreamJoinTest|KTableKTableForeignKeyJoinIntegrationTest|RelationalSmokeTest|MockProcessorContextStateStoreTest|TopologyTestDriverTest|IQv2StoreIntegrationTest).java"/>
              files="(KStreamSlidingWindowAggregateTest|KStreamKStreamLeftJoinTest|KStreamKStreamOuterJoinTest|KTableKTableForeignKeyJoinIntegrationTest).java"/>
import org.apache.kafka.streams.Topology;

import java.util.Collections;
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15623 (2/n) migrate remaining tests in streams module to junit 5 (integration & internals) (#16360).

**Test**
Test modified in AbstractJoinIntegrationTest.java covering the corrected behaviour.
