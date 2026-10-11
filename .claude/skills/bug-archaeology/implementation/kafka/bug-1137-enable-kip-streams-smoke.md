**Commit**: 5c63b4569b · **Ticket**: KAFKA-19202
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Enables KIP-1071 (`group.protocol=streams`) in the first streams  system
test `streams_smoke_test.py`. All tests using KIP-1071 cannot use `KafkaTest` anymore, since  we need
to customize the broker configuration. The corresponding  functionality
is added to `BaseStreamsTest`, which all streams  tests will have to
extend from now on. There are some left-overs from ZK in the tests that I copied   from
'KafkaTest'.

**Before/After**
```java
// Before
                 use_share_groups=None
            override_configs[config_property.GROUP_COORDINATOR_REBALANCE_PROTOCOLS] = 'classic,consumer,share'
    def __init__(self, test_context, kafka, command, processing_guarantee = 'at_least_once', num_threads = 3, replication_factor = 3):

// After
UNSTABLE_API_VERSIONS_ENABLE = "unstable.api.versions.enable"

                 use_share_groups=None,
                 use_streams_groups=False
        :param use_streams_groups: When true, enables the use of streams groups introduced in KIP-1071
        self.use_streams_groups = use_streams_groups
        enabledProtocols = 'classic,consumer'

            enabledProtocols += ',share'

        if self.use_streams_groups is True:
            override_configs[config_property.UNSTABLE_API_VERSIONS_ENABLE] = str(True)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19202: enable kip-1071 in streams_smoke_test.py (#19560).

**Test**
Test modified in config_property.py covering the corrected behaviour.
