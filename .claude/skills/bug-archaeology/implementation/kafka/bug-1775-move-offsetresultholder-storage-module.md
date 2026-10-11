**Commit**: e8863c9ee2 · **Ticket**: KAFKA-18180
**Subsystem**: core/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Christo Lolov <lolovc@amazon.com>

**Before/After**
```java
// Before
import scala.util.Either;
    public AsyncOffsetReadFutureHolder<Either<Exception, Option<FileRecords.TimestampAndOffset>>> asyncOffsetRead(
        CompletableFuture<Either<Exception, Option<FileRecords.TimestampAndOffset>>> taskFuture = new CompletableFuture<>();
import scala.util.Either;
import scala.util.Left;
import scala.util.Right;
    private final Supplier<Option<FileRecords.TimestampAndOffset>> searchInLocalLog;
    private final Consumer<Either<Exception, Option<FileRecords.TimestampAndOffset>>> callback;
                                 Consumer<Either<Exception, Option<FileRecords.TimestampAndOffset>>> callback) {
        this.searchInLocalLog = searchInLocalLog;
        Either<Exception, Option<FileRecords.TimestampAndOffset>> result;
            Option<FileRecords.TimestampAndOffset> timestampAndOffsetOpt =

// After
import org.apache.kafka.storage.internals.log.OffsetResultHolder;
    public AsyncOffsetReadFutureHolder<OffsetResultHolder.FileRecordsOrError> asyncOffsetRead(
        CompletableFuture<OffsetResultHolder.FileRecordsOrError> taskFuture = new CompletableFuture<>();
import org.apache.kafka.storage.internals.log.OffsetResultHolder;
import java.util.Optional;
    private final Supplier<Optional<FileRecords.TimestampAndOffset>> searchInLocalLog;
    private final Consumer<OffsetResultHolder.FileRecordsOrError> callback;
                                 Consumer<OffsetResultHolder.FileRecordsOrError> callback) {
        this.searchInLocalLog = () -> OptionConverters.toJava(searchInLocalLog.get());
        OffsetResultHolder.FileRecordsOrError result;
```

**How to spot**
When reviewing core/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18180: move offsetresultholder to storage module (#18100).

**Test**
Test modified in RemoteLogOffsetReaderTest.java covering the corrected behaviour.
