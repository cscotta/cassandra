**Commit**: 0a4a12fbc4 · **Ticket**: KAFKA-17225
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
The initial drop of ShareMembershipManager contained a lot of code duplicated from MembershipManagerImpl. The plan was always to share as much code as possible between the membership managers for consumer groups and share groups. This issue refactors the membership managers so that almost all of the code is in common. Reviewers:  Lianet Magrans <lianetmr@gmail.com>,  Chia Chuan Yu <yujuan476@gmail.com>, Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.internals.NetworkClientDelegate.PollResult;
import org.apache.kafka.clients.consumer.internals.Utils.TopicIdPartitionComparator;
import org.apache.kafka.clients.consumer.internals.Utils.TopicPartitionComparator;
import org.apache.kafka.clients.consumer.internals.events.BackgroundEventHandler;
import org.apache.kafka.clients.consumer.internals.events.CompletableBackgroundEvent;
import org.apache.kafka.clients.consumer.internals.events.ConsumerRebalanceListenerCallbackCompletedEvent;
import org.apache.kafka.clients.consumer.internals.events.ConsumerRebalanceListenerCallbackNeededEvent;
import org.apache.kafka.clients.consumer.internals.events.ErrorEvent;
import org.apache.kafka.common.message.ConsumerGroupHeartbeatResponseData;
import org.apache.kafka.common.metrics.Metrics;

// After
import org.apache.kafka.clients.consumer.Consumer;
import java.util.Objects;
 * A stateful object tracking the state of a single member in relationship to a group:
 * Responsible for:
 * <li>Keeping member state</li>
 * <li>Keeping assignment for the member</li>
 * <li>Computing assignment for the group if the member is required to do so<li/>
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17225: refactor consumer membership managers (#16751).

**Test**
Test modified in AsyncKafkaConsumerTest.java covering the corrected behaviour.
