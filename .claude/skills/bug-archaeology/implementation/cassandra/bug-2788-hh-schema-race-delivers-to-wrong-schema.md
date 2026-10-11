# Bug 2788: Hinted Handoff Delivers Hints Before Schema Agreement

**Commit**: ecbd71f6b4 · **Ticket**: CASSANDRA-2083

## Detection Rule
When a hinted handoff delivery method sends mutations to a recovered endpoint without first verifying the remote node has the same schema version as the local node, data may be applied against an incompatible schema.

## Code Smell
`deliverHintsToEndpoint` proceeds directly to replay hints without checking gossip application state for schema agreement. The hint delivery loop starts immediately after confirming the endpoint is alive, with no schema version comparison.

## Tags
- scope: cassandra
- category: race-condition
- code-pattern: missing-schema-agreement-check
- severity: silent-wrong-result
- subsystem: storage-engine

## Root Cause
When a node rejoins the cluster after a schema migration, gossip propagates schema version changes asynchronously. If hinted handoff begins replaying mutations immediately upon endpoint liveness, the target may still be applying schema updates. Mutations carrying column families or types that differ from the remote node's current schema can be silently misapplied or cause errors. The fix introduces `waitForSchemaAgreement()` that polls gossip's `ApplicationState.SCHEMA` value comparing remote to local, and also adds a random stagger sleep to prevent thundering-herd replay from multiple replicas simultaneously.

## Example
```java
// BEFORE: No schema check before delivering hints
private void deliverHintsToEndpoint(InetAddress endpoint)
        throws IOException, DigestMismatchException, InvalidRequestException, TimeoutException {
    logger_.info("Started hinted handoff for endpoint " + endpoint);
    queuedDeliveries.remove(endpoint);
    // ... immediately starts reading and replaying hints
}
```
```java
// AFTER: Schema agreement enforced before delivery
private void deliverHintsToEndpoint(InetAddress endpoint)
        throws IOException, DigestMismatchException, InvalidRequestException, TimeoutException, InterruptedException {
    logger_.info("Checking remote schema before delivering hints");
    int waited = waitForSchemaAgreement(endpoint);
    if (waited == 0) {
        int sleep = new Random().nextInt(60000);
        Thread.sleep(sleep);
    }
    if (!Gossiper.instance.getEndpointStateForEndpoint(endpoint).isAlive()) {
        logger_.info("Endpoint {} died before hint delivery, aborting", endpoint);
        return;
    }
    logger_.info("Started hinted handoff for endpoint " + endpoint);
    queuedDeliveries.remove(endpoint);
}
```
