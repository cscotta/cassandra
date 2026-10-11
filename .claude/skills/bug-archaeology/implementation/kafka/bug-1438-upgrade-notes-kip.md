**Commit**: c465cf6b4b · **Ticket**: KAFKA-17298
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: David Jacot <djacot@confluent.io>

**Before/After**
```java
// Before
    public static final String GROUP_REMOTE_ASSIGNOR_DOC = "The server-side assignor to use. If no assignor is specified, " +
        "the group coordinator will pick one. This configuration is applied only if <code>group.protocol</code> is " +
        "set to \"consumer\".";

// After
    public static final String GROUP_REMOTE_ASSIGNOR_DOC = "The name of the server-side assignor to use. " +
        "If not specified, the group coordinator will pick the first assignor defined in the broker config group.consumer.assignors." +
        "This configuration is applied only if <code>group.protocol</code> is set to \"consumer\".";
  <h3 class="anchor-heading"><a id="consumer_rebalance_protocol" class="anchor-link"></a><a href="#consumer_rebalance_protocol">6.10 Consumer Rebalance Protocol</a></h3>

  <h4 class="anchor-heading"><a id="consumer_rebalance_protocol_overview" class="anchor-link"></a><a href="#consumer_rebalance_protocol_overview">Overview</a></h4>

  <p>Starting from Apache Kafka 4.0, the Next Generation of the Consumer Rebalance Protocol (<a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-848%3A+The+Next+Generation+of+the+Consumer+Rebalance+Protocol">KIP-848</a>)
    is Generally Available (GA). It improves the scalability of consumer groups while simplifying consumers. It also decreases rebalance times, thanks to
    its fully incremental design, which no longer relies on a global synchronization barrier.</p>

  <p>Consumer Groups using the new protocol are now referred to as <code>Consumer</code> groups, while groups using the old protocol are referred to as <code>Classic</code> groups. Note
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17298: update upgrade notes for 4.0 kip-848 (#18756).

**Test**
No test added.
