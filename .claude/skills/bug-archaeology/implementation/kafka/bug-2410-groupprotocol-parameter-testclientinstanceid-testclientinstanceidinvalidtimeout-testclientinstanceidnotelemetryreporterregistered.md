**Commit**: e0bf580185 · **Ticket**: KAFKA-17311
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    public void testClientInstanceId() {
    public void testClientInstanceIdInvalidTimeout() {
    public void testClientInstanceIdNoTelemetryReporterRegistered() {

// After
    public void testClientInstanceId(GroupProtocol groupProtocol) {
        props.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, groupProtocol.name());
    public void testClientInstanceIdInvalidTimeout(GroupProtocol groupProtocol) {
        props.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, groupProtocol.name());

    public void testClientInstanceIdNoTelemetryReporterRegistered(GroupProtocol groupProtocol) {
        props.setProperty(ConsumerConfig.GROUP_PROTOCOL_CONFIG, groupProtocol.name());
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17311 add groupprotocol parameter to testclientinstanceid, testclientinstanceidinvalidtimeout, and testclientinsta.

**Test**
Test modified in KafkaConsumerTest.java covering the corrected behaviour.
