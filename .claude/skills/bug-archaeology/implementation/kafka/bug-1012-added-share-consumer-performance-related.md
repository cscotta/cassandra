**Commit**: 942b11bb36 · **Ticket**: KAFKA-19321
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
This PR includes some performance system tests utilizing the
kafka-share-consumer-perf.sh tool for share groups

Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
from kafkatest.services.performance import ProducerPerformanceService, EndToEndLatencyService, ConsumerPerformanceService, throughput, latency, compute_aggregate_throughput

// After
from ducktape.utils.util import wait_until
from kafkatest.services.performance import ProducerPerformanceService, EndToEndLatencyService, ConsumerPerformanceService, ShareConsumerPerformanceService, throughput, latency, compute_aggregate_throughput
from kafkatest.services.security.security_config import SecurityConfig
import os


    @cluster(num_nodes=8)
    @matrix(security_protocol=['SSL'], interbroker_security_protocol=['PLAINTEXT'], tls_version=['TLSv1.2', 'TLSv1.3'],
            compression_type=["none", "snappy"], metadata_quorum=[quorum.isolated_kraft], use_share_groups=[True])
    @matrix(security_protocol=['PLAINTEXT'], compression_type=["none", "snappy"], metadata_quorum=[quorum.isolated_kraft],
            use_share_groups=[True])
    def test_producer_and_share_consumer(self, compression_type="none", security_protocol="PLAINTEXT", tls_version=None,
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19321: added share_consumer_performance.py and related system tests (#19836).

**Test**
Test modified in benchmark_test.py covering the corrected behaviour.
