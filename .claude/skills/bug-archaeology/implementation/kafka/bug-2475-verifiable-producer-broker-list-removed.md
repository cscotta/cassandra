**Commit**: 464051929d · **Ticket**: KAFKA-17388
**Subsystem**: tools/VerifiableProducer
**Category**: Wrong constant / default value

**Root cause**
`VerifiableProducer` accepted both `--broker-list` and `--bootstrap-server`, with the tool preferring `--bootstrap-server` if both were present. Keeping `--broker-list` alive allowed legacy scripts to silently succeed without migrating. Additionally, the Python system-test service always passed `--broker-list`, even for Kafka versions that already supported `--bootstrap-server`, because there was no version-aware flag selection. The fix removes `--broker-list` from the tool and adds version-aware flag selection to the Python service.

**Before/After**
```java
// Before (Java tool)
if (res.get("bootstrapServer") != null) {
    producerProps.put(BOOTSTRAP_SERVERS_CONFIG, res.getString("bootstrapServer"));
} else if (res.getString("brokerList") != null) {
    producerProps.put(BOOTSTRAP_SERVERS_CONFIG, res.getString("brokerList"));
}

// After
if (res.get("bootstrapServer") == null) { parser.printHelp(); System.exit(0); }
producerProps.put(BOOTSTRAP_SERVERS_CONFIG, res.getString("bootstrapServer"));
```

**How to spot**
After removing a deprecated CLI option from a Java tool, verify that all Python system-test service wrappers that invoke that tool have also been updated. If a wrapper unconditionally passes the old flag, the system tests will fail with unrecognized option errors after the upgrade.

**Test**
`testConfigBrokerList` and `testBrokerListOverride` removed from `VerifiableProducerTest`.
