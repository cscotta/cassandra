**Commit**: a0b716ec9f · **Ticket**: KAFKA-16001
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Completely migrates ConsumerNetworkThreadTest away from ConsumerTestBuilder and removes all usages of spy objects and replaced with mocks. Removes testEnsureMetadataUpdateOnPoll() since it was doing integration testing. Also I adds new tests to get more complete test coverage of ConsumerNetworkThread. Reviewers: Kirk True <kirk@kirktrue.pro>, Lianet Magrans <lianetmr@gmail.com>, Philip Nee <pnee@confluent.io>, Matthias J.

**Before/After**
```java
// Before
import org.apache.kafka.clients.MockClient;
import org.apache.kafka.clients.consumer.internals.events.CompletableApplicationEvent;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.message.FindCoordinatorRequestData;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.requests.FindCoordinatorRequest;
import org.apache.kafka.common.requests.FindCoordinatorResponse;
import org.apache.kafka.common.requests.MetadataResponse;
import org.apache.kafka.common.requests.OffsetCommitRequest;
import org.apache.kafka.common.requests.OffsetCommitResponse;
import org.apache.kafka.common.requests.RequestTestUtils;

// After
import org.apache.kafka.common.utils.LogContext;
import org.apache.kafka.common.utils.MockTime;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16001: migrated consumernetworkthreadtest away from consumertestbuilder (#16140).

**Test**
Test modified in ConsumerNetworkThreadTest.java covering the corrected behaviour.
