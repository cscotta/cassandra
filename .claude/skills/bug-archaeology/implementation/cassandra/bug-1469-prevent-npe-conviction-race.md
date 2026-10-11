# Bug 1469: Prevent NPE on FD Conviction Race

**Commit**: 05920cb38b · **Ticket**: CASSANDRA-7940
**Subsystem**: src/java/org/apache/cassandra/gms/Gossiper.java
**Category**: Missing null / bounds check

---

## Root Cause

`Gossiper.convict()` is called by the failure detector when it decides an endpoint should be marked dead. Between the failure detector firing and `convict()` executing, the endpoint could be removed from `endpointStateMap` (e.g. via a concurrent `removeEndpoint` during node decommissioning or ring change). The method did not guard against a null result from `endpointStateMap.get(endpoint)`, so a subsequent call to `epState.isAlive()` threw a NullPointerException.

### Before / After

```java
// BEFORE
public void convict(InetAddress endpoint, double phi) {
    EndpointState epState = endpointStateMap.get(endpoint);
    if (epState.isAlive() && !isDeadState(epState))  // NPE if epState == null
        markDead(endpoint, epState);
```

```java
// AFTER
public void convict(InetAddress endpoint, double phi) {
    EndpointState epState = endpointStateMap.get(endpoint);
    if (epState == null)
        return;
    if (epState.isAlive() && !isDeadState(epState))
        markDead(endpoint, epState);
```

---

## How to Spot

Any map lookup whose key can be concurrently removed must null-check the result before use. In gossip/membership code, endpoint state is inherently volatile; every code path that fetches from `endpointStateMap` should treat a null result as "endpoint already gone" and return early rather than proceeding.

---

## Test

No test added.
