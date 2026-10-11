**Commit**: adf4b6eb39 · **Ticket**: KAFKA-17991
**Subsystem**: share/src
**Category**: Race condition / missing synchronization

**Root cause**
Reviewers: Andrew Schofield <aschofield@confluent.io>, Apoorv Mittal <apoorvmittal10@gmail.com>, David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
import java.util.concurrent.ExecutionException;
    private WriteShareGroupStateResult writeResponsesToResult(
                            WriteShareGroupStateResponse partitionResponse = future.get();
                        } catch (InterruptedException | ExecutionException e) {
    private ReadShareGroupStateResult readResponsesToResult(
                            ReadShareGroupStateResponse partitionResponse = future.get();
                        } catch (InterruptedException | ExecutionException e) {

// After
    /**
     * Takes in a list of COMPLETED futures and combines the results,
     * taking care of errors if any, into a single WriteShareGroupStateResult
     * @param futureMap - HashMap of {topic -> {part -> future}}
     * @return Object representing combined result of type WriteShareGroupStateResult
     */
    // visible for testing
    WriteShareGroupStateResult writeResponsesToResult(
                            // already completed because of allOf application in the caller
                            WriteShareGroupStateResponse partitionResponse = future.join();
                        } catch (Exception e) {
    /**
```

**How to spot**
When reviewing share/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17991: added timed calls for future.get in persister. (#17772).

**Test**
Test modified in DefaultStatePersisterTest.java covering the corrected behaviour.
