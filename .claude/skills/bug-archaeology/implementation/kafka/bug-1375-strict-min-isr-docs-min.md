**Commit**: 269e2d898b · **Ticket**: KAFKA-18849
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
KIP-966 adds strict min ISR rule, so this PR improves the docs of min.insync.replicas to include that change. Reviewers: Ismael Juma <ismael@juma.me.uk>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        "then the producer will raise an exception (either NotEnoughReplicas or " +
        "NotEnoughReplicasAfterAppend).<br>When used together, <code>min.insync.replicas</code> and <code>acks</code> " +
        "allow you to enforce greater durability guarantees. A typical scenario would be to " +
        "create a topic with a replication factor of 3, set <code>min.insync.replicas</code> to 2, and " +
        "produce with <code>acks</code> of \"all\". This will ensure that the producer raises an exception " +
        "if a majority of replicas do not receive a write.";
    public static final String MIN_IN_SYNC_REPLICAS_DOC = "When a producer sets acks to \"all\" (or \"-1\"), " +
            "<code>min.insync.replicas</code> specifies the minimum number of replicas that must acknowledge " +
            "a write for the write to be considered successful. If this minimum cannot be met, " +
            "then the producer will raise an exception (either <code>NotEnoughReplicas</code> or " +
            "<code>NotEnoughReplicasAfterAppend</code>).<br>When used together, <code>min.insync.replicas</code> and acks " +
            "allow you to enforce greater durability guarantees. A typical scenario would be to " +

// After
        "then the producer will raise an exception (either <code>NotEnoughReplicas</code> or <code>NotEnoughReplicasAfterAppend</code>).<br> " +
        "Regardless of the <code>acks</code> setting, the messages will not be visible to the consumers until " +
        "they are replicated to all in-sync replicas and the <code>min.insync.replicas</code> condition is met.<br> " +
        "When used together, <code>min.insync.replicas</code> and <code>acks</code> allow you to enforce greater durability guarantees. " +
        "A typical scenario would be to create a topic with a replication factor of 3, " +
        "set <code>min.insync.replicas</code> to 2, and produce with <code>acks</code> of \"all\". " +
        "This will ensure that a majority of replicas must persist a write before it's considered successful by the producer and it's visible to consumers.";
    public static final String MIN_IN_SYNC_REPLICAS_DOC = TopicConfig.MIN_IN_SYNC_REPLICAS_DOC;
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18849 add "strict min isr" to the docs of "min.insync.replicas" (#19016).

**Test**
No test added.
