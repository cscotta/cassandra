**Commit**: 8ce514a52e · **Ticket**: KAFKA-16534
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
This change implements the KRaft voter sending UpdateVoter request. The
UpdateVoter RPC is used to update a voter's listeners and supported
kraft versions. The UpdateVoter RPC is sent if the replicated voter set
(VotersRecord in the log) doesn't match the local voter's supported
kraft versions and controller listeners. To not starve the Fetch request, the UpdateVoter request is sent at most
every 3 fetch timeouts.

**Before/After**
```java
// Before
    { "name": "ClusterId", "type": "string", "versions": "0+" },
    private final Endpoints endpoints;
        Endpoints endpoints,
        this.endpoints = endpoints;
        return endpoints;
        return endpoints
                        endpoints

// After
    { "name": "ClusterId", "type": "string", "versions": "0+", "nullableVersions": "0+" },
    private final Endpoints leaderEndpoints;
    // Used to throttle update voter request and allow for Fetch/FetchSnapshot requests
    private final Timer updateVoterPeriodTimer;
        Endpoints leaderEndpoints,
        this.leaderEndpoints = leaderEndpoints;
        this.updateVoterPeriodTimer = time.timer(updateVoterPeriodMs());
        return leaderEndpoints;
        return leaderEndpoints
                        leaderEndpoints
    private long updateVoterPeriodMs() {
        // Allow for a few rounds of fetch request before attempting to update
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-16534; implemeent update voter sending (#16837).

**Test**
Test modified in KafkaRaftClientReconfigTest.java covering the corrected behaviour.
