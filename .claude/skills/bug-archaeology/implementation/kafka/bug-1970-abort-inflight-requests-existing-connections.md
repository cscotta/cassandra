**Commit**: 0f33b16fdf · **Ticket**: KAFKA-18085
**Subsystem**: clients/src
**Category**: Logic error in condition

**Root cause**
When disconnecting channels before rebootstrapping due to the rebootstrap conditions introduced in KIP-1102, we should ensure that inflight requests are aborted similar to other disconnections like request timeout in clients. With the earlier rebootstrapping from KIP-899, we only rebootstrapped when there were no connections, so no disconnections are required. Reviewers: Manikumar Reddy <manikumar.reddy@gmail.com>

**Before/After**
```java
// Before
    /**
     * Closes connections to all nodes. All requests on the connections will be cleared.  ClientRequest
     * callbacks will not be invoked for the cleared requests, nor will they be returned from poll().
     */
    void closeAll();

    @Override
    public void closeAll() {
        log.info("Client requested connection close from all nodes.");
        List<Node> nodes = this.metadataUpdater.fetchNodes();
        for (Node node : nodes) {
            close(node.idString());

// After
    /**
     * Returns true if metadata couldn't be fetched for `rebootstrapTriggerMs` or if server requested rebootstrap.
     *
     * @param now Current time in milliseconds
     * @param rebootstrapTriggerMs Configured timeout after which rebootstrap is triggered
     */
    default boolean needsRebootstrap(long now, long rebootstrapTriggerMs) {
        return false;
    }

    /**
     * Performs rebootstrap, replacing the existing cluster with the bootstrap cluster.
```

**How to spot**
When reviewing clients/src code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18085: abort inflight requests on existing connections while rebootstrapping (#17939).

**Test**
Test modified in MockClient.java covering the corrected behaviour.
