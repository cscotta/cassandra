**Commit**: b649b1ed5d · **Ticket**: KAFKA-18935
**Subsystem**: clients/src/main/java/org/apache/kafka/common/requests/FetchResponse.java
**Category**: Missing null / bounds check

**Root cause**
The broker-side `FetchResponse` and `ShareFetchResponse` constructors accepted `FetchResponseData` objects whose `partitionData.records()` fields could be `null`. Downstream consumers (clients, tests, benchmarks) assumed records were never null and dereferenced the field directly, producing `NullPointerExceptions`. The schema definition used `nullableVersions: "0+"` which permitted null in every version, making null a legal but practically unusable value.

**Before/After**
```java
// BEFORE – constructor left null records untouched
public FetchResponse(FetchResponseData fetchResponseData) { ... }

// AFTER – server-side factory method normalises null to EMPTY
public static FetchResponse of(FetchResponseData data) {
    for (FetchResponseData.FetchableTopicResponse response : data.responses()) {
        for (FetchResponseData.PartitionData partition : response.partitions()) {
            if (partition.records() == null)
                partition.setRecords(MemoryRecords.EMPTY);
        }
    }
    return new FetchResponse(data);   // constructor now private
}
```

**How to spot**
Any place that iterates `FetchResponse.responseData()` and calls `.records()` without a null check is vulnerable. Search for callers that do `partition.records().sizeInBytes()` or pass the result directly to a `MemoryRecords` parameter. The pattern is also present in `ShareFetchResponse` where the same nullability was allowed.

**Test**
`RequestResponseTest.testSerialization`: exercises round-trip serialisation for all API versions; previously skipped `UNREGISTER_BROKER v0`. The ShareFetch-side tests in `ShareSessionHandlerTest` were updated to assert `MemoryRecords.EMPTY` instead of `assertNull`.
