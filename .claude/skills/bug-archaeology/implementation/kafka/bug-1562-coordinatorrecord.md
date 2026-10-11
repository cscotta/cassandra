**Commit**: b368c38684 · **Ticket**: KAFKA-18302
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This patch does a few things:
1) Replace ApiMessageAndVersion by ApiMessage in CoordinatorRecord for the key
2) Leverage the fact that ApiMessage exposes the apiKey. Hence we don't need to specify the key anymore. Reviewers: Andrew Schofield <aschofield@confluent.io>

**Before/After**
```java
// Before
    public static ByteBuffer toCoordinatorTypePrefixedByteBuffer(final short type, final Message message) {
        bytes.writeShort(type);
    public static byte[] toCoordinatorTypePrefixedBytes(final short type, final Message message) {
        ByteBuffer buffer = toCoordinatorTypePrefixedByteBuffer(type, message);
 * A Record which contains an {{@link ApiMessageAndVersion}} as key and
    private final ApiMessageAndVersion key;
    public CoordinatorRecord(
        ApiMessageAndVersion key,
    public ApiMessageAndVersion key() {

        CoordinatorRecord record = (CoordinatorRecord) o;


// After
    public static ByteBuffer toCoordinatorTypePrefixedByteBuffer(final ApiMessage message) {
        if (message.apiKey() < 0) {
            throw new IllegalArgumentException("Cannot serialize a message without an api key.");
        }
        bytes.writeShort(message.apiKey());
    public static byte[] toCoordinatorTypePrefixedBytes(final ApiMessage message) {
        ByteBuffer buffer = toCoordinatorTypePrefixedByteBuffer(message);
import org.apache.kafka.common.protocol.ApiMessage;
 * A Record which contains an {{@link ApiMessage}} as key and

    public static CoordinatorRecord record(
        ApiMessage key,
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18302; update coordinatorrecord (#18512).

**Test**
Test modified in CoordinatorRecordTest.java covering the corrected behaviour.
