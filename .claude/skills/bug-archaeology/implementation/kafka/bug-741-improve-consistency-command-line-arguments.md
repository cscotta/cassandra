**Commit**: dbe9d34e47 · **Ticket**: KAFKA-19624
**Subsystem**: tools/src
**Category**: Logic error in condition

**Root cause**
resolves https://issues.apache.org/jira/browse/KAFKA-19624

Reviewers: @brandboat, @AndrewJSchofield, @m1a2st

**Before/After**
```java
// Before
        "consumer.config", "Consumer config properties file."
            'topic': self.topic,
            'messages': self.messages
        cmd += " --consumer.config %s" % ConsumerPerformanceService.CONFIG_FILE
        "consumer.config", "Consumer config properties file."
    def args(self):
            'messages': self.messages,
        for key, value in self.args().items():
        cmd += " --consumer.config %s" % ShareConsumerPerformanceService.CONFIG_FILE
            AtomicLong totalMessagesRead = new AtomicLong(0);

// After
        "command-config", "Config properties file."
            'topic': self.topic
        if version.supports_command_config():
            args['num-records'] = self.messages
        else:
            args['messages'] = self.messages

        if node.version.supports_command_config():
            cmd += " --command-config %s" % ConsumerPerformanceService.CONFIG_FILE
        else:
            cmd += " --consumer.config %s" % ConsumerPerformanceService.CONFIG_FILE
        "command-config", "Config properties file."
```

**How to spot**
When reviewing tools/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19624: improve consistency of command-line arguments for consumer performance tests (#20385).

**Test**
Test modified in consumer_performance.py covering the corrected behaviour.
