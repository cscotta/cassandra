**Commit**: c7f7169772 · **Ticket**: KAFKA-19978
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
Since internal topics are created asynchronous, and we do not want to
have heartbeats failing, when an application has not yet created its
internal topics, it remains in NOT_READY, until the internal topics are
created. The way the client-side is implemented right now, the Kafka Streams
application will still  enter RUNNING state with an empty assignment,
when internal topics are missing. From the outside, it will therefore
look like the initial set-up of the application has finished, while the
application is still waiting for the group to become ready. This propagates the state of the group as part of the assignment, so
that  we delay entering a "RUNNING" state until the group has become
re...

**Before/After**
```java
// Before
            Collections.emptyMap()
                               final Map<String, SortedSet<Integer>> warmupTasks) {
                                             final Map<String, SortedSet<Integer>> warmupTasks) {
                    warmupTasks.equals(this.warmupTasks)) {
            return Optional.of(new LocalAssignment(nextLocalEpoch, activeTasks, standbyTasks, warmupTasks));
                Objects.equals(warmupTasks, that.warmupTasks);
            return Objects.hash(localEpoch, activeTasks, standbyTasks, warmupTasks);


// After
            Collections.emptyMap(),
            false
        public final boolean isGroupReady;
                               final Map<String, SortedSet<Integer>> warmupTasks,
                               final boolean isGroupReady) {
            this.isGroupReady = isGroupReady;
                                             final Map<String, SortedSet<Integer>> warmupTasks,
                                             final boolean isGroupReady) {
                    warmupTasks.equals(this.warmupTasks) &&
                    isGroupReady == this.isGroupReady
            ) {
            return Optional.of(new LocalAssignment(nextLocalEpoch, activeTasks, standbyTasks, warmupTasks, isGroupReady));
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19978: don't enter running state as long as group is not ready (#21110).

**Test**
Test modified in StreamsGroupHeartbeatRequestManagerTest.java covering the corrected behaviour.
