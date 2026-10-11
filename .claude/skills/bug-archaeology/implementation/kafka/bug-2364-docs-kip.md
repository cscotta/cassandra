**Commit**: aee44efd40 · **Ticket**: KAFKA-17048
**Subsystem**: config/kraft
**Category**: Logic error in condition

**Root cause**
Change the configurations under config/kraft to use controller.quorum.bootstrap.servers instead of controller.quorum.voters. Add comments explaining how to use the older static quorum configuration where appropriate. In docs/ops.html, remove the reference to "tentative timelines for ZooKeeper removal" and "Tiered storage is considered as an early access feature" since they are no longer up-to-date. Add KIP-853 information.

**Before/After**
```java
// Before
# The connect string for the controller quorum
controller.quorum.voters=1@localhost:9093
# The connect string for the controller quorum
controller.quorum.voters=1@localhost:9093
# Note that only the controller listeners are allowed here when `process.roles=controller`, and this listener should be consistent with `controller.quorum.voters` value.
# The connect string for the controller quorum
    <h5 class="anchor-heading"><a id="zk_depr_timeline" class="anchor-link"></a><a href="#zk_depr_timeline">ZooKeeper and KRaft timeline</a></h5>
    <p>For details and updates on tentative timelines for ZooKeeper removal and planned KRaft feature releases, refer to <a href="https://cwiki.apache.org/confluence/display/KAFKA/KIP-833%3A+Mark+KRaft+as+Production+Ready">KIP-833</a>.</p>

  <p>All of the servers in a Kafka cluster discover the quorum voters using the <code>controller.quorum.voters</code> property. This identifies the quorum controller servers that should be used. All the controllers must be enumerated. Each controller is identified with their <code>id</code>, <code>host</code> and <code>port</code> information. For example:</p>
  <pre><code class="language-bash">controller.quorum.voters=id1@host1:port1,id2@host2:port2,id3@host3:port3</code></pre>

// After
# Information about the KRaft controller quorum.
controller.quorum.bootstrap.servers=localhost:9093
# Information about the KRaft controller quorum.
# Uncomment controller.quorum.voters to use a static controller quorum.
#controller.quorum.voters=1@localhost:9093
controller.quorum.bootstrap.servers=localhost:9093
# Note that only the controller listeners are allowed here when `process.roles=controller`
# Information about the KRaft controller quorum.
# Uncomment controller.quorum.voters to use a static controller quorum.
#controller.quorum.voters=1@localhost:9093
  <p>All of the servers in a Kafka cluster discover the active controller using the <code>controller.quorum.bootstrap.servers</code> property. All the controllers should be enumerated in this property. Each controller is identified with their <code>host</code> and <code>port</code> information. For example:</p>
  <pre><code class="language-bash">controller.quorum.bootstrap.servers=host1:port1,host2:port2,host3:port3</code></pre>
```

**How to spot**
When reviewing config/kraft code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17048; update docs for kip-853 (#17076).

**Test**
No test added.
