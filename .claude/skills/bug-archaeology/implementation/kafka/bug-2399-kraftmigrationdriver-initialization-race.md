**Commit**: 0e30209f01 · **Ticket**: KAFKA-17506
**Subsystem**: metadata/src
**Category**: Race condition / missing synchronization

**Root cause**
There is a race condition between KRaftMigrationDriver running its first poll() and being notified by Raft about a leader change. If onControllerChange is called before RecoverMigrationStateFromZKEvent is run, we will end up getting stuck in the INACTIVE state. This patch fixes the race by enqueuing a RecoverMigrationStateFromZKEvent from onControllerChange if the driver has not yet initialized. If another RecoverMigrationStateFromZKEvent was already enqueued, the second one to run will just be ignored.

**Before/After**
```java
// Before
            if (migrationState.equals(MigrationDriverState.INACTIVE)) {
                // No need to log anything if this node is not the active controller

// After
        if (migrationState.equals(MigrationDriverState.UNINITIALIZED)) {
            eventQueue.append(new RecoverMigrationStateFromZKEvent());
        }
            if (EnumSet.of(MigrationDriverState.UNINITIALIZED, MigrationDriverState.INACTIVE).contains(migrationState)) {
                // No need to log anything if this node is not the active controller or the driver has not initialized
    @Test
    public void testOnControllerChangeWhenUninitialized() throws InterruptedException {
        CountingMetadataPropagator metadataPropagator = new CountingMetadataPropagator();
        CapturingMigrationClient.newBuilder().build();
        CapturingMigrationClient migrationClient = CapturingMigrationClient.newBuilder().build();
        MockFaultHandler faultHandler = new MockFaultHandler("testBecomeLeaderUninitialized");
        KRaftMigrationDriver.Builder builder = defaultTestBuilder()
```

**How to spot**
When reviewing metadata/src code, look for race condition / missing synchronization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17506 kraftmigrationdriver initialization race (#17147).

**Test**
Test modified in KRaftMigrationDriverTest.java covering the corrected behaviour.
