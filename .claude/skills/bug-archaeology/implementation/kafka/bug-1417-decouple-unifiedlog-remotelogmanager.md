**Commit**: 0a2fab9310 · **Ticket**: KAFKA-14484
**Subsystem**: core/src
**Category**: State not cleaned up

**Root cause**
Reviewers: Kamal Chandraprakash <kamal.chandraprakash@gmail.com>, Ismael Juma <ismael@juma.me.uk>

**Before/After**
```java
// Before
import scala.Option;
public class RemoteLogManager implements Closeable {
            Long timestamp,
            Long startingOffset,
            Supplier<Option<FileRecords.TimestampAndOffset>> searchLocalLog) {
import scala.Option;
import scala.jdk.javaapi.OptionConverters;

                                 Supplier<Option<FileRecords.TimestampAndOffset>> searchInLocalLog,
        this.searchInLocalLog = () -> OptionConverters.toJava(searchInLocalLog.get());
            // NOTE: All the exceptions from the secondary storage are catched instead of only the KafkaException.
import org.apache.kafka.storag

// After
import org.apache.kafka.storage.internals.log.AsyncOffsetReader;
public class RemoteLogManager implements Closeable, AsyncOffsetReader {
    @Override
            long timestamp,
            long startingOffset,
            Supplier<Optional<FileRecords.TimestampAndOffset>> searchLocalLog) {
                                 Supplier<Optional<FileRecords.TimestampAndOffset>> searchInLocalLog,
        this.searchInLocalLog = searchInLocalLog;
            // NOTE: All the exceptions from the secondary storage are caught instead of only the KafkaException.
```

**How to spot**
When reviewing core/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14484: decouple unifiedlog and remotelogmanager (#18460).

**Test**
Test modified in RemoteLogOffsetReaderTest.java covering the corrected behaviour.
