**Commit**: 7779a632a9 · **Ticket**: KAFKA-20163
**Subsystem**: connect/mirror
**Category**: Logic error in condition

**Root cause**
This patch adds the following missing configuration in MM2 exclude
defaults that were introduced in KIP-937:

- log.message.timestamp.before.max.ms
- log.message.timestamp.after.max.ms

Reviewers: Luke Chen <showuon@gmail.com>

Signed-off-by: Federico Valeri <fedevaleri@gmail.com>

**Before/After**
```java
// Before
    public static final String CONFIG_PROPERTIES_EXCLUDE_DEFAULT = "follower\\.replication\\.throttled\\.replicas, "
                                                                   + "leader\\.replication\\.throttled\\.replicas, "
                                                                   + "message\\.timestamp\\.difference\\.max\\.ms, "
                                                                   + "message\\.timestamp\\.type, "
                                                                   + "unclean\\.leader\\.election\\.enable, "
                                                                   + "min\\.insync\\.replicas";
        filterConfig.put(DefaultConfigPropertyFilter.CONFIG_PROPERTIES_EXCLUDE_CONFIG, "follower\\.replication\\.throttled\\.replicas, "
                + "leader\\.replication\\.throttled\\.replicas, "
                + "message\\.timestamp\\.difference\\.max\\.ms, "
                + "message\\.timestamp\\.type, "
                + "unclean\\.leader\\.election\\.enable, "
                + "min\\.insync\\.replicas,"

// After
    public static final String CONFIG_PROPERTIES_EXCLUDE_DEFAULT = "follower.replication.throttled.replicas, "
                                                                   + "leader.replication.throttled.replicas, "
                                                                   + "message.timestamp.difference.max.ms, "
                                                                   + "log.message.timestamp.before.max.ms, "
                                                                   + "log.message.timestamp.after.max.ms, "
                                                                   + "message.timestamp.type, "
                                                                   + "unclean.leader.election.enable, "
                                                                   + "min.insync.replicas";
        filterConfig.put(DefaultConfigPropertyFilter.CONFIG_PROPERTIES_EXCLUDE_CONFIG, "follower.replication.throttled.replicas, "
                + "leader.replication.throttled.replicas, "
                + "message.timestamp.difference.max.ms, "
                + "log.message.timestamp.before.max.ms, "
```

**How to spot**
When reviewing connect/mirror code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20163: missing mm2 default exclude configs (#21443).

**Test**
Test modified in MirrorSourceConnectorTest.java covering the corrected behaviour.
