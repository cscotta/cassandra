**Commit**: 005155ba5c · **Ticket**: KAFKA-17406
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import kafka.common.ClientIdAndBroker
import org.apache.kafka.common.{InvalidRecordException, TopicPartition, Uuid}
  private val metricId = ClientIdAndBroker(clientId, leader.brokerEndPoint().host, leader.brokerEndPoint().port)
package kafka.common

/**
 * contributor license agreements.  See the NOTICE file distributed with
 * the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
trait ClientIdBroker {
}
case class ClientIdAndBroker(clientId: String, brokerHost: String, brokerPort: Int) extends ClientIdBroker {

// After
import org.apache.kafka.common.{ClientIdAndBroker, InvalidRecordException, TopicPartition, Uuid}
  private val metricId = new ClientIdAndBroker(clientId, leader.brokerEndPoint().host, leader.brokerEndPoint().port)
/*
 * contributor license agreements. See the NOTICE file distributed with
 * the License. You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
package org.apache.kafka.common;
public class ClientIdAndBroker {
    public final String clientId;
    public final String brokerHost;
    public final int brokerPort;
    public ClientIdAndBroker(String clientId, String brokerHost, int brokerPort) {
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17406: move clientidandbroker to server-common module (#16967).

**Test**
No test added.
