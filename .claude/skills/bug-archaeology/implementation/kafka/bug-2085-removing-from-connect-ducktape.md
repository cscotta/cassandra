**Commit**: 5ec9dffa81 · **Ticket**: KAFKA-17916
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Migrates existing connect tests that were using Zookeeper to use KRaft
instead, and cleans up some dead ZK code. For broker compatibility tests,
tests for versions 2.1-2.3 still need to use ZK. Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
    def test_dynamic_logging(self):
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(LATEST_2_3), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(LATEST_2_2), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(LATEST_2_1), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(LATEST_2_0), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(LATEST_1_1), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(LATEST_1_0), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned')
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=False, connect_protocol='sessioned')
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible')
    @parametrize(broker_version=str(LATEST_2_3), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible')
    @parametrize(broker_version=str(LATEST_2_2), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible')

// After
    @parametrize(metadata_quorum=quorum.isolated_kraft)
    def test_dynamic_logging(self, metadata_quorum):
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned', metadata_quorum=quorum.isolated_kraft)
    @parametrize(broker_version=str(LATEST_2_3), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned', metadata_quorum=quorum.zk)
    @parametrize(broker_version=str(LATEST_2_2), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned', metadata_quorum=quorum.zk)
    @parametrize(broker_version=str(LATEST_2_1), auto_create_topics=False, exactly_once_source=True, connect_protocol='sessioned', metadata_quorum=quorum.zk)
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=False, connect_protocol='sessioned', metadata_quorum=quorum.isolated_kraft)
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible', metadata_quorum=quorum.isolated_kraft)
    @parametrize(broker_version=str(LATEST_2_3), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible', metadata_quorum=quorum.zk)
    @parametrize(broker_version=str(LATEST_2_2), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible', metadata_quorum=quorum.zk)
    @parametrize(broker_version=str(LATEST_2_1), auto_create_topics=False, exactly_once_source=False, connect_protocol='compatible', metadata_quorum=quorum.zk)
    @parametrize(broker_version=str(DEV_BRANCH), auto_create_topics=False, exactly_once_source=False, connect_protocol='
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17916: removing zk from connect ducktape tests (#17689).

**Test**
Test modified in connect_distributed_test.py covering the corrected behaviour.
