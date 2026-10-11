**Commit**: da61b19aae · **Ticket**: KAFKA-19981
**Subsystem**: storage/api
**Category**: Resource leak

**Root cause**
This PR distinguishes between `RemoteStorageException` and
`RetriableRemoteStorageException`   in `RemoteLogManager` to handle
temporary storage degradations gracefully:

1. Copy path: Avoids deleting partially uploaded segments when
`RetriableRemoteStorageException` is thrown;
2. Delete path: Skips incrementing failedRemoteDeleteRequestRate metric
for retriable exceptions;
3. Documentation: Updates `RemoteStorageManager` Javadoc to clarify
exception usage;
4.

**Before/After**
```java
// Before
     * @throws RemoteStorageException if there are any errors in storing the data of the segment.
                                                LogSegmentData logSegmentData)
            throws RemoteStorageException;
            } catch (RetriableException ex) {
        protected void execute(UnifiedLog log) throws InterruptedException {
        public void copyLogSegmentsToRemote(UnifiedLog log) throws InterruptedException {

// After
 * <p>
 * Plugin implementors of {@link RemoteStorageManager} should throw {@link RetriableRemoteStorageException}
 * for transient errors that can be recovered by retrying. For non-recoverable errors,
 * {@link RemoteStorageException} should be thrown. This distinction allows RemoteLogManager to
 * handle retries gracefully and report metrics accurately.
     * @throws RemoteStorageException          if there are any errors in storing the data of the segment.
     * @throws RetriableRemoteStorageException if the error is transient and the operation can be retried.
                                                LogSegmentData logSegmentData) throws RemoteStorageException;
     * @throws RetriableRemoteStorageException if the error is transient and the operation can be retried.
            } catch (RetriableException | RetriableRemoteStorageException ex) {
        protected void execute(UnifiedLog log) throws InterruptedException, RetriableRemoteStorageException {
        public void copyLogSegmentsToRemote(UnifiedLog log) throws InterruptedException, RetriableRemoteStorageException {
```

**How to spot**
When reviewing storage/api code, look for resource leak patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19981: handle retriable remote storage exception in remotelogmanager (#21150).

**Test**
Test modified in RemoteStorageManager.java covering the corrected behaviour.
