**Commit**: 77be6f2d74 · **Ticket**: KAFKA-19053
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Removed the unused FetchResponse#of that is not used in production. The
test cases that originally invoked this method have been updated to call
the other

[FetchResponse#of](https://github.com/apache/kafka/blob/6af849f8642d93877e64dace272cee1551620862/clients/src/main/java/org/apache/kafka/common/requests/FetchResponse.java#L232),
which is currently used by ```KafkaApis```, to maintain the integrity of
the tests. Reviewers: Jun Rao <junrao@gmail.com>, PoAn Yang <payang@apache.org>,
 Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    // TODO: remove as a part of KAFKA-12410
    public static FetchResponse of(Errors error,
                                   int throttleTimeMs,
                                   int sessionId,
                                   LinkedHashMap<TopicIdPartition, FetchResponseData.PartitionData> responseData) {
        return new FetchResponse(toMessage(error, throttleTimeMs, sessionId, responseData.entrySet().iterator(), Collections.emptyList()));
    }

                        new RespEntry("foo", 1, fooId, 0, 0)));
                        new RespEntry("foo", 1, fooId, 10, 20)));
                respMap(new RespEntry("foo", 1, fooId, 20, 20)));
                respMap());

// After
                        new RespEntry("foo", 1, fooId, 0, 0)),
                    List.of());
                        new RespEntry("foo", 1, fooId, 10, 20)),
                    List.of());
                respMap(new RespEntry("foo", 1, fooId, 20, 20)),
                    List.of());
                    respMap(),
                    List.of());
                        new RespEntry("bar", 0, barId, 10, 20)),
                    List.of());
            FetchResponse resp2 = FetchResponse.of(Errors.FETCH_SESSION_ID_NOT_FOUND, 0, INVALID_SESSION_ID,
                    respMap(),
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19053 remove fetchresponse#of which is not used in production … (#19327).

**Test**
Test modified in FetchSessionHandlerTest.java covering the corrected behaviour.
