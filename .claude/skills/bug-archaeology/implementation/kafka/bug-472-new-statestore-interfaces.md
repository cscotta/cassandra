**Commit**: b59dcca4e9 · **Ticket**: KAFKA-17411
**Subsystem**: streams/src
**Category**: Logic error in condition

**Root cause**
These are the new interfaces detailed in KIP-1035: "StateStore managed
changelog offsets". This PR introduces the interfaces changes, but makes otherwise no
consequential behavioural changes. Outside of `StateStore.java`, _all_ changes are essentially just a
rename of all invocations of `StateStore#flush` to instead call
`StateStore#commit`. The `changelogOffsets` being passed in these invocations is currently
unused: the behaviour of `StateStore#commit` remains identical to
`StateStore#flush` before these changes.

**Before/After**
```java
// Before
                        state.flush();
                        public void flush() {
                        store.flush();

// After
import org.apache.kafka.common.TopicPartition;
                        public void commit(final Map<TopicPartition, Long> changelogOffsets) {
import org.apache.kafka.common.TopicPartition;
```

**How to spot**
When reviewing streams/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17411: add new statestore interfaces (#20955).

**Test**
Test modified in EosIntegrationTest.java covering the corrected behaviour.
