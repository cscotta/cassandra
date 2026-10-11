**Commit**: 9f9836c0c4 · **Ticket**: KAFKA-20376
**Subsystem**: docs/operations
**Category**: Logic error in condition

**Root cause**
Adding more info about assignors, evolution  timeline. Updating existing
info with latest state. Reviewers: David Jacot <david.jacot@gmail.com>, Andrew Schofield
 <aschofield@confluent.io>

**Before/After**
```java
// Before
Starting from Apache Kafka 4.0, the Next Generation of the Consumer Rebalance Protocol ([KIP-848](https://cwiki.apache.org/confluence/x/HhD1D)) is Generally Available (GA). It improves the scalability of consumer groups while simplifying consumers. It also decreases rebalance times, thanks to its fully incremental design, which no longer relies on a global synchronization barrier.
Consumer Groups using the new protocol are now referred to as `Consumer` groups, while groups using the old protocol are referred to as `Classic` groups. Note that Classic groups can still be used to form consumer groups using the old protocol.
The new consumer protocol is automatically enabled on the server since Apache Kafka 4.0. Enabling and disabling the protocol is controlled by the `group.version` feature flag.
The assignment strategy is also controlled by the server. The `group.consumer.assignors` configuration can be used to specify the list of available assignors for `Consumer` groups. By default, the `uniform` assignor and the `range` assignor are configured. The first assignor in the list is used by default unless the Consumer selects a different one. It is also possible to implement custom assignment strategies on the server side by implementing the `ConsumerGroupPartitionAssignor` interface and specifying the full class name in the configuration.
## Consumer
Since Apache Kafka 4.0, the Consumer supports the new consumer rebalance protocol. However, the protocol is not enabled by default. The `group.protocol` configuration must be set to `consumer` to enable it. When enabled, the new consumer protocol is used alongside an improved threading model.
The `group.remote.assignor` configuration is introduced as an optional configuration to overwrite the default assignment strategy configured on the server side.
The `subscribe(SubscriptionPattern)` and `subscribe(SubscriptionPattern, ConsumerRebalanceListener)` methods have been added to subscribe to a regular expression with the new consumer rebalance protocol. With these methods, the regular expression uses the RE2J format and is now evaluated on the server side.

// After
Starting from Apache Kafka 4.0, the Next Generation of the Consumer Rebalance Protocol ([KIP-848](https://cwiki.apache.org/confluence/x/HhD1D)) is Generally Available (GA), ready for production workloads.
It improves the scalability of consumer groups while simplifying consumers. It also decreases rebalance times, thanks to its fully incremental design, which no longer relies on a global synchronization barrier.
There are now two types of consumer groups, named depending upon whether the consumers are using the new "consumer" group protocol or the earlier "classic" group protocol.
The new consumer protocol is automatically enabled on the server since Apache Kafka 4.0. Enabling and disabling the protocol on the server side is controlled by the `group.version` feature flag.
The assignment strategy is also controlled by the server. The `group.consumer.assignors` configuration can be used to specify the list of available assignors for `Consumer` groups.
* `uniform` and `range` assignors are provided by default
* `uniform` is the default one (first assignor in the list of `group.consumer.assignors`) unless the Consumer selects a different one (via the client config `group.remote.assignor`)
* it is possible to implement custom assignment strategies on the server side, by implementing the `ConsumerGroupPartitionAssignor` interface and specifying the full class name in the `group.consumer.assignors` configuration.
### Migrating from Client-side Assignors

The following table shows the mapping from client-side assignors to the new server-side assignors:

```

**How to spot**
When reviewing docs/operations code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20376: update consumer rebalance docs (#21930).

**Test**
No test added.
