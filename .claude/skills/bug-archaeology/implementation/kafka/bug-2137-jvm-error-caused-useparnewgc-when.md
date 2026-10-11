**Commit**: db25c212ed · **Ticket**: KAFKA-17883
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        cmd = "export LOG_DIR=%s;" % ConsumerPerformanceService.LOG_DIR
        cmd = "export KAFKA_LOG4J_OPTS=\"-Dlog4j.configuration=file:%s\"; " % EndToEndLatencyService.LOG4J_CONFIG
        cmd = ""

// After
from kafkatest.services.kafka.util import fix_opts_for_new_jvm
        cmd = fix_opts_for_new_jvm(node)
        cmd += "export LOG_DIR=%s;" % ConsumerPerformanceService.LOG_DIR
from kafkatest.services.kafka.util import fix_opts_for_new_jvm
        cmd = fix_opts_for_new_jvm(node)
        cmd += "export KAFKA_LOG4J_OPTS=\"-Dlog4j.configuration=file:%s\"; " % EndToEndLatencyService.LOG4J_CONFIG
from kafkatest.services.kafka.util import fix_opts_for_new_jvm
        cmd = fix_opts_for_new_jvm(node)
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17883 fix jvm error caused by useparnewgc when running old kafka client in e2e (#17612).

**Test**
Test modified in consumer_performance.py covering the corrected behaviour.
