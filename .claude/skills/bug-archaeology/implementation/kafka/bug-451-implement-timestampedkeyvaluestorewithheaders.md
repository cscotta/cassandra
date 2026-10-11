**Commit**: 85c6313be0 · **Ticket**: KAFKA-20132
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
This PR implements the changelogging layer of the
`TimestampedKeyValueStoreWithHeaders` introduced in KIP-1271. Reviewers: TengYao Chi <frankvicky@apache.org>, Matthias J. Sax
 <matthias@confluent.io>

**Before/After**
```java
// Before
import org.apache.kafka.common.header.internals.RecordHeaders;
        final Headers headers;
        if (!consistencyEnabled) {
            headers = null;
        } else {
            // Add the vector clock to the header part of every record
            headers = new RecordHeaders();
            headers.add(ChangelogRecordDeserializationHelper.CHANGELOG_VERSION_HEADER_RECORD_CONSISTENCY);
            headers.add(new RecordHeader(ChangelogRecordDeserializationHelper.CHANGELOG_POSITION_HEADER_KEY,
                    PositionSerde.serialize(position).array()));

// After
import org.apache.kafka.common.header.Headers;
                          final Headers headers,
import org.apache.kafka.common.header.Headers;
                   final Headers headers,
                          final Headers headers,
        if (consistencyEnabled) {
            addVectorClockToHeaders(headers, position);
    private void addVectorClockToHeaders(final Headers headers, final Position position) {
        headers.add(ChangelogRecordDeserializationHelper.CHANGELOG_VERSION_HEADER_RECORD_CONSISTENCY);
        headers.add(new RecordHeader(ChangelogRecordDeserializationHelper.CHANGELOG_POSITION_HEADER_KEY,
            PositionSerde.serialize(position).array()));
    }
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-20132: implement timestampedkeyvaluestorewithheaders (4/n) (#21454).

**Test**
Test modified in AbstractProcessorContextTest.java covering the corrected behaviour.
