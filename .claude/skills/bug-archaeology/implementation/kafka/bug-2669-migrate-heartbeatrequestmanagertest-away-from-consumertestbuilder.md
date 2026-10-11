**Commit**: cca0390822 · **Ticket**: KAFKA-15999
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
In this PR I have completely migrated HeartbeatRequestManagerTest away from ConsumerTestBuilder. I have removed any instances of spy objects and used mocks wherever possible. 31/31 tests are passing. I also removed one test that existed in another test file.

**Before/After**
```java
// Before
import org.apache.kafka.common.message.ConsumerGroupHeartbeatResponseData.Assignment;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.junit.jupiter.api.AfterEach;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.consumer.internals.ConsumerTestBuilder.DEFAULT_GROUP_INSTANCE_ID;
import static org.apache.kafka.clients.consumer.internals.ConsumerTestBuilder.DEFAULT_HEARTBEAT_INTERVAL_MS;
import static org.apache.kafka.clients.consumer.internals.ConsumerTestBuilder.DEFAULT_MAX_POLL_INTERVAL_MS;
import static org.apache.kafka.clients.consumer.internals.ConsumerTestBuilder.DEFAULT_REMOTE_ASSIGNOR;

// After
import java.util.Set;

    private static final String DEFAULT_REMOTE_ASSIGNOR = "uniform";
    private static final String DEFAULT_GROUP_INSTANCE_ID = "group-instance-id";
    private static final int DEFAULT_HEARTBEAT_INTERVAL_MS = 1000;
    private static final int DEFAULT_MAX_POLL_INTERVAL_MS = 10000;
    private static final long DEFAULT_RETRY_BACKOFF_MS = 80;
    private static final long DEFAULT_RETRY_BACKOFF_MAX_MS = 1000;
    private static final double DEFAULT_HEARTBEAT_JITTER_MS = 0.0;
    private static final String DEFAULT_MEMBER_ID = "member-id";
    private static final int DEFAULT_MEMBER_EPOCH = 1;
    private LogContext logContext;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15999 migrate heartbeatrequestmanagertest away from consumertestbuilder (#16200).

**Test**
Test modified in HeartbeatRequestManagerTest.java covering the corrected behaviour.
