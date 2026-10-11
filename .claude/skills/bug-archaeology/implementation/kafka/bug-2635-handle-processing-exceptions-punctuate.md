**Commit**: f1ef7a5a9f · **Ticket**: KAFKA-16448
**Subsystem**: checkstyle/suppressions.xml
**Category**: Resource leak

**Root cause**
This PR is part of KIP-1033 which aims to bring a ProcessingExceptionHandler to Kafka Streams in order to deal with exceptions that occur during processing. This PR actually catches processing exceptions from punctuate. Co-authored-by: Dabz <d.gasparina@gmail.com>
Co-authored-by: loicgreffier <loic.greffier@michelin.com>

Reviewers: Bruno Cadonna <bruno@confluent.io>, Matthias J. Sax <matthias@confluent.io>

**Before/After**
```java
// Before
              files="(KafkaStreams|KStreamImpl|KTableImpl|InternalTopologyBuilder|StreamsPartitionAssignor|StreamThread|IQv2StoreIntegrationTest|KStreamImplTest|RocksDBStore).java"/>
        } catch (final StreamsException e) {
        } catch (final RuntimeException e) {
            throw new StreamsException(String.format("%sException caught while punctuating processor '%s'", logPrefix, node.name()), e);

// After
              files="(KafkaStreams|KStreamImpl|KTableImpl|InternalTopologyBuilder|StreamsPartitionAssignor|StreamThread|IQv2StoreIntegrationTest|KStreamImplTest|RocksDBStore|StreamTask).java"/>
import org.apache.kafka.streams.errors.ErrorHandlerContext;
import org.apache.kafka.streams.errors.internals.DefaultErrorHandlerContext;
import static org.apache.kafka.streams.StreamsConfig.PROCESSING_EXCEPTION_HANDLER_CLASS_CONFIG;
    private final Sensor droppedRecordsSensor;
        droppedRecordsSensor = TaskMetrics.droppedRecordsSensor(threadId, taskId, streamsMetrics);
        } catch (final FailedProcessingException e) {
            throw createStreamsException(node.name(), e.getCause());
        } catch (final TaskCorruptedException | TaskMigratedException e) {
        } catch (final Exception e) {
            final ErrorHandlerContext errorHandlerContext = new DefaultErrorHandlerContext(
                null,
```

**How to spot**
When reviewing checkstyle/suppressions.xml code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16448: handle processing exceptions in punctuate (#16300).

**Test**
Test modified in StreamTaskTest.java covering the corrected behaviour.
