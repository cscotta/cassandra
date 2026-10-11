**Commit**: 43e22ef5d6 · **Ticket**: KAFKA-19093
**Subsystem**: core/src
**Category**: Resource leak

**Root cause**
> INFO [data-plane Kafka Request Handler on Broker 3000], Resizing
request handler thread pool size from 8 to 10
(kafka.server.KafkaRequestHandlerPool)

it should be "Controller" rather than "Broker"

Reviewers: Ken Huang <s7133700@gmail.com>, Chia-Ping Tsai
 <chia7712@gmail.com>

**Before/After**
```java
// Before
  this.logIdent = s"[Kafka Request Handler $id on ${nodeName.capitalize} $brokerId], "
  this.logIdent = "[" + logAndThreadNamePrefix + " Kafka Request Handler on Broker " + brokerId + "], "

// After
  this.logIdent = s"[Kafka Request Handler $id on ${nodeName.capitalize} $brokerId] "
  this.logIdent = s"[$logAndThreadNamePrefix Kafka Request Handler on ${nodeName.capitalize} $brokerId] "
```

**How to spot**
When reviewing core/src code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19093 change the "handler on broker" to "handler on controller" for controller server (#19384).

**Test**
No test added.
