**Commit**: 78aff4fede · **Ticket**: KAFKA-18659
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Return produce v0-v2 as supported versions in `ApiVersionsResponse`, but disable support
for it everywhere else. Since clients pick the highest supported version by both client and broker during version
negotiation, this solves the problem with minimal tech debt (even though it's not ideal that
`ApiVersionsResponse` becomes inconsistent with the actual protocol support). Add one test for the socket server handling (in `ProcessorTest`) and one test for the
client behavior (in `ProduceRequestTest`). Adjust a couple of api versions tests to verify
the new behavior.

**Before/After**
```java
// Before
        short oldestVersion = oldestVersion();
            .filter(apiKey -> apiKey.toApiVersion(false).isPresent())
        List<ApiKeys> apis = Arrays.stream(ApiKeys.values())
            .filter(apiKey -> apiKey.inScope(ApiMessageType.ListenerType.BROKER))
            .collect(Collectors.toList());
        return EnumSet.copyOf(apis);
            for (int i = 0; i < responses.length; i++) {
                Schema schema = responses[i];

// After
    // Versions 0-2 were removed in Apache Kafka 4.0, version 3 is the new baseline. Due to a bug in librdkafka,
    // version `0` has to be included in the api versions response (see KAFKA-18659). In order to achieve that,
    // we adjust `toApiVersion` to return `0` for the min version of `produce` in the broker listener.
    public static final short PRODUCE_API_VERSIONS_RESPONSE_MIN_VERSION = 0;

    /**
     * To workaround a critical bug in librdkafka, the api versions response is inconsistent with the actual versions
     * supported by `produce` - this method handles that. It should be called in the context of the api response protocol
     * handling.
     *
     * It should not be used by code generating protocol documentation - we keep that consistent with the actual versions
     * supported by `produce`.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18659: librdkafka compressed produce fails unless api versions returns produce v0 (#18727).

**Test**
Test modified in ApiVersionsResponseTest.java covering the corrected behaviour.
