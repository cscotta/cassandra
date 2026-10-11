**Commit**: 50009cc76a · **Ticket**: KAFKA-19635
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Updates to `docs/upgrade.html` for

https://cwiki.apache.org/confluence/display/KAFKA/KIP-1147:+Improve+consistency+of+command-line+arguments. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
    <li>The <code>org.apache.kafka.disallowed.login.modules</code> config was deprecated. Please use the <code>org.apache.kafka.allowed.login.modules</code>
    <li>The <code>KafkaPrincipalBuilder</code> now extends <code>KafkaPrincipalSerde</code>. Force developer to implement <code>KafkaPrincipalSerde</code> interface for custom <code>KafkaPrincipalBuilder</code>.
        The <code>ConsumerPerformance</code> command line tool has a new <code>include</code> option that is alternative to the <code>topic</code> option.

// After
    <li>
        The <code>org.apache.kafka.disallowed.login.modules</code> config was deprecated. Please use the <code>org.apache.kafka.allowed.login.modules</code>
    <li>
        The <code>KafkaPrincipalBuilder</code> now extends <code>KafkaPrincipalSerde</code>. Force developer to implement <code>KafkaPrincipalSerde</code> interface for custom <code>KafkaPrincipalBuilder</code>.
        The <code>ConsumerPerformance</code> command line tool has a new <code>--include</code> option that is an alternative to the <code>--topic</code> option.
    <li>
        The consistency of argument names for the command-line tools has been improved (<a href="https://cwiki.apache.org/confluence/x/DguWF">KIP-1147</a>).
        The deprecated options will be removed in Kafka 5.0.
        <ul>
            <li>
                In <code>kafka-producer-perf-test.sh</code>, <code>--bootstrap-server</code> and <code>--reporting-interval</code> option are added.
            </li>
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19635: kip-1147 changes for upgrade.html (#20415).

**Test**
No test added.
