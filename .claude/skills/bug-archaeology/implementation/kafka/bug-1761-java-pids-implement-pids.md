**Commit**: 4567f39825 · **Ticket**: KAFKA-18343
**Subsystem**: tests/kafkatest
**Category**: Logic error in condition

**Root cause**
Related to KAFKA-18343,

Currently, there is an issue that the ps ax output is truncated, which causes the Kafka process ID to be unavailable. This issue can be mitigated by replacing ps ax with jcmd (i.e. using java_pids in ducktape), as it does not suffer from the truncation problem. Reviewers: Justine Olshan <jolshan@confluent.io>

**Before/After**
```java
// Before
            cmd = "ps ax | grep -i %s | grep -v grep | awk '{print $1}'" % self.java_class_name()
            pid_arr = [pid for pid in node.account.ssh_capture(cmd, allow_fail=True, callback=int)]
            return pid_arr
        return "kafka.Kafka"

// After
            return node.account.java_pids(self.java_class_name())
        return "kafka\.Kafka"
```

**How to spot**
When reviewing tests/kafkatest code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18343: use java_pids to implement pids (#18312).

**Test**
Test modified in kafka.py covering the corrected behaviour.
