**Commit**: 49d7ea6c6a · **Ticket**: KAFKA-16308
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This change includes:

1. Dependency checking when updating the feature (all request versions)
2. Returning top level error and no feature level errors if any feature failed to update and using this error for all the features in the response. (all request versions)
3.

**Before/After**
```java
// Before
                        for (final UpdatableFeatureResult result : response.data().results()) {
                            final KafkaFutureImpl<Void> future = updateFutures.get(result.feature());
                            if (future == null) {
                                log.warn("Server response mentioned unknown feature {}", result.feature());
                            } else {
                                final Errors error = Errors.forCode(result.errorCode());
                                if (error == Errors.NONE) {
                                    future.complete(null);
                                    future.completeExceptionally(error.exception(result.errorMessage()));
                        // The server should send back a response for every feature, but we do a sanity check anyway.
                        completeUnrealizedFutures(updateFutures.entrySet().stream(),
                            feature -> "The controller response did not contain a result for feature " + feature);

// After
                        // For V2 and above, None responses will just have a top level NONE error -- mark all the futures as completed.
                        if (response.data().results().isEmpty()) {
                            for (final KafkaFutureImpl<Void> future : updateFutures.values()) {
                                future.complete(null);
                            }
                        } else {
                            for (final UpdatableFeatureResult result : response.data().results()) {
                                final KafkaFutureImpl<Void> future = updateFutures.get(result.feature());
                                if (future == null) {
                                    log.warn("Server response mentioned unknown feature {}", result.feature());
                                    final Errors error = Errors.forCode(result.errorCode());
                                    if (error == Errors.NONE) {
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16308 [3/n]: introduce feature dependency validation to updatefeatures command (#16443).

**Test**
Test modified in KafkaAdminClientTest.java covering the corrected behaviour.
