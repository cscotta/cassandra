**Commit**: 2f0ae82d4a · **Ticket**: KAFKA-12989
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                } else {
                    AbstractRequest abstractRequest = request.requestBuilder().build(version);
                    if (!futureResp.requestMatcher.matches(abstractRequest))
                        throw new IllegalStateException("Request matcher did not match next-in-line request "
                                + abstractRequest + " with prepared response " + futureResp.responseBody);
            assertEquals(findCoordinatorRequest.data().key(), transactionalId);
    client.prepareUnsupportedVersionResponse(req => req.apiKey == requestHeader.apiKey)

// After

                AbstractRequest abstractRequest = request.requestBuilder().build(version);
                if (!futureResp.requestMatcher.matches(abstractRequest))
                    throw new IllegalStateException("Request matcher did not match next-in-line request "
                            + abstractRequest + " with prepared response " + futureResp.responseBody);

            assertTrue(findCoordinatorRequest.data().key().isEmpty());
            assertEquals(1, findCoordinatorRequest.data().coordinatorKeys().size());
            assertTrue(findCoordinatorRequest.data().coordinatorKeys().contains(transactionalId));
    val isEnvelopeRequest: RequestMatcher = request => request.isInstanceOf[EnvelopeRequest]
    client.prepareUnsupportedVersionResponse(isEnvelopeRequest)
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-12989 mockclient should respect the request matcher passed to prepareunsupportedversionresponse (#16849).

**Test**
Test modified in MockClient.java covering the corrected behaviour.
