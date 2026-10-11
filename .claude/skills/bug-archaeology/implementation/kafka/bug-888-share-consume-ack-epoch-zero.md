**Commit**: 56a3c6dde9 · **Ticket**: KAFKA-19485

**Subsystem**: Share Groups / ShareConsumeRequestManager

**Category**: Logic error in condition

**Root cause**
`ShareConsumeRequestManager` included piggybacked acknowledgements in the initial share-fetch request (session epoch = 0). The share protocol requires that acknowledgements only accompany requests after the session is established (epoch > 0); sending them on epoch 0 violates the protocol and can confuse the broker. Additionally, the manager did not correctly handle the edge case where a metadata refresh returned an empty topic list. The fix adds `maybeAddAcknowledgements` guards at two call sites to skip piggybacking when the epoch is 0 and adds handling for the empty-topics metadata case.

**Before/After**
```java
// Before: acknowledgements added regardless of epoch
ShareFetchRequest.Builder builder = createFetchRequest();
maybeAddAcknowledgements(builder); // added even when epoch == 0

// After: acknowledgements only on established sessions
ShareFetchRequest.Builder builder = createFetchRequest();
if (shareSessionEpoch > 0) {
    maybeAddAcknowledgements(builder);
}
```

**How to spot**
Piggybacking logic that does not check the current session epoch before appending acknowledgements will violate the share protocol on session initialization. Always guard acknowledgement piggyback calls with an `epoch > 0` check.

**Test**
`ShareConsumeRequestManagerTest` adds a test verifying that the initial request (epoch=0) contains no piggybacked acknowledgements even when pending acks exist.
