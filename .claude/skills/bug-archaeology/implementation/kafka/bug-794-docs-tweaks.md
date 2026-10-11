**Commit**: e6f3efc914 · **Ticket**: KAFKA-19635
**Subsystem**: docs/upgrade.html
**Category**: Logic error in condition

**Root cause**
Improve the wording in the upgrade doc slightly. Also fix a tiny
annoyance in the output from the message generator. Reviewers: Apoorv Mittal <apoorvmittal10@gmail.com>

**Before/After**
```java
// Before
                        <code>kafka-consumer-perf-test.sh</code> and <code>kafka-share-consumer-perf-test.sh</code> gain
                        <code>--command-property</code> to bring all performance testing tools in line
        System.out.printf("MessageGenerator: processed %d Kafka message JSON files(s).%n", numProcessed);

// After
                        <code>kafka-consumer-perf-test.sh</code> and <code>kafka-share-consumer-perf-test.sh</code> gain the
                        <code>--command-property</code> option to bring all performance testing tools in line
        System.out.printf("MessageGenerator: processed %d Kafka message JSON file(s).%n", numProcessed);
```

**How to spot**
When reviewing docs/upgrade.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19635: minor docs tweaks (#20434).

**Test**
No test added.
