**Commit**: 0ff55c316a · **Ticket**: KAFKA-18106
**Subsystem**: metadata/src
**Category**: State not cleaned up

**Root cause**
Generate LeaderAndISR change records when a broker re-registers and the quorum controller detects an unclean shutdown. This is necessary to ensure that we perform the expected partition state transitions, eg: bumping leader epochs and so on. Reviewers: Colin P. McCabe <cmccabe@apache.org>

**Before/After**
```java
// Before
        return ControllerResult.atomicOf(records, new BrokerRegistrationReply(record.brokerEpoch()));
        registerMaybeFenceStaleBroker(sessionTimeoutNs);
     * @param sessionTimeoutNs      The broker session timeout in nanoseconds.
    private void registerMaybeFenceStaleBroker(long sessionTimeoutNs) {
            maybeFenceStaleBrokerPeriodNs(sessionTimeoutNs),

// After
        // A broker registration that cleans up a previous incarnation's unclean shutdown may generate a large number of records.
        // It is safe to return these records as a non-atomic batch as long as the registration record is added last.
        // This ensures that in case of a controller failure, the broker will re-register and the new controller
        // can retry the unclean shutdown cleanup.
        return ControllerResult.of(records, new BrokerRegistrationReply(record.brokerEpoch()));
        private OptionalLong fenceStaleBrokerIntervalNs = OptionalLong.empty();
        public Builder setFenceStaleBrokerIntervalNs(long fenceStaleBrokerIntervalNs) {
            this.fenceStaleBrokerIntervalNs = OptionalLong.of(fenceStaleBrokerIntervalNs);
            return this;
        }

                    fenceStaleBrokerIntervalNs,
```

**How to spot**
When reviewing metadata/src code, look for state not cleaned up patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18106: generate leaderandisrupdates on unclean shutdown (#18045).

**Test**
Test modified in QuorumControllerTest.java covering the corrected behaviour.
