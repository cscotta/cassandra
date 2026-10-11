**Commit**: 7ddd0d7cce · **Ticket**: KAFKA-19703
**Subsystem**: docs/streams
**Category**: Logic error in condition

**Root cause**
Removed versions 2.3 and below from UpgradeFromValues, including all the
usagesof them. Reviewers: Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
            Note that as of 2.3, you need to do two things to enable optimizations. In addition to setting this config to <code>StreamsConfig.OPTIMIZE</code>, you'll need to pass in your
          when upgrading from below version 2.0, or when upgrading to 2.4+ from any version lower than 2.4.
        (possible values are <code>"0.10.0" - "3.4"</code>) and during the second you remove it. This is required to safely handle 3 changes. The first is introduction of the new cooperative rebalancing protocol of the embedded consumer. The second is a change in foreign-key join serialization format.
        Note that you will remain using the old eager rebalancing protocol if you skip or delay the second rolling bounce, but you can safely switch over to cooperative at any time once the entire group is on 2.4+ by removing the config value and bouncing. For more details please refer to
        <a href="https://cwiki.apache.org/confluence/x/vAclBg">KIP-429</a>. The third is a change in the serialization format for an internal repartition topic. For more details, please refer to <a href="https://cwiki.apache.org/confluence/x/P5VbDg">KIP-904</a>:
    <p> As an alternative, an offline upgrade is also possible. Upgrading from any versions as old as 0.10.0.x to {{fullDotVersion}} in offline mode require the following steps: </p>
        <li> stop all old (e.g., 0.10.0.x) application instances </li>
    <p>
        Note: The cooperative rebalancing protocol has been the default since 2.4, but we have continued to support the
        eager rebalancing protocol to provide users an upgrade path. This support will be dropped in a future release,
        so any users still on the eager protocol should prepare to finish upgrading their applications to the cooperative protocol in version 3.1.
        This only affects users who are still on a version older than 2.4, and users wh

// After
            Note that you need to do two things to enable optimizations. In addition to setting this config to <code>StreamsConfig.OPTIMIZE</code>, you'll need to pass in your
          when upgrading to 3.4+ from any version lower than 3.4.
        (possible values are <code>"2.4" - "3.4"</code>) and during the second you remove it. This is required to safely handle 2 changes. The first is a change in foreign-key join serialization format.
        The second is a change in the serialization format for an internal repartition topic. For more details, please refer to <a href="https://cwiki.apache.org/confluence/x/P5VbDg">KIP-904</a>:
    <p> As an alternative, an offline upgrade is also possible. Upgrading from any versions as old as 0.11.0.x to {{fullDotVersion}} in offline mode require the following steps: </p>
        <li> stop all old (e.g., 0.11.0.x) application instances </li>
```

**How to spot**
When reviewing docs/streams code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19703: removed versions 2.3 and below from upgradefromvalues. (#20539).

**Test**
Test modified in StreamsPartitionAssignorTest.java covering the corrected behaviour.
