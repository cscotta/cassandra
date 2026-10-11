**Commit**: eb9cfb06c0 · **Ticket**: KAFKA-17412
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
        "loss.";
    public static final String UNCLEAN_LEADER_ELECTION_ENABLE_DOC = "Indicates whether to enable replicas not in the ISR set to be elected as leader as a last resort, even though doing so may result in data loss";

// After
        "loss.<p>Note: In KRaft mode, when enabling this config dynamically, it needs to wait for the unclean leader election" +
        "thread to trigger election periodically (default is 5 minutes). Please run `kafka-leader-election.sh` with `unclean` option " +
         "to trigger the unclean leader election immediately if needed.</p>";
        <li><code>unclean.leader.election.enable</code> config is supported in KRaft. Compared with ZK mode, there is one behavior change in KRaft mode
            when dynamically enabling <code>unclean.leader.election.enable</code> config. Please check
            <a href="https://kafka.apache.org/documentation/#brokerconfigs_unclean.leader.election.enable">here</a> for more details.</li>
    public static final String UNCLEAN_LEADER_ELECTION_ENABLE_DOC = "Indicates whether to enable replicas not in the ISR set to be elected as leader as a last resort, even though doing so may result in data loss" +
            "<p>Note: In KRaft mode, when enabling this config dynamically, it needs to wait for the unclean leader election " +
            "thread to trigger election periodically (default is 5 minutes). Please run `kafka-leader-election.sh` with `unclean` option " +
            "to trigger the unclean leader election immediately if needed.</p>";
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17412: add doc for `unclean.leader.election.enable` in kraft (#17051).

**Test**
No test added.
