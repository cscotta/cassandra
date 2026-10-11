# Bug 0711: NPE in StorageService When Gossip State Is Absent

**Commit**: fee2a3a095 · **Ticket**: CASSANDRA-13060
**Subsystem**: Gossip / StorageService
**Category**: Missing null / bounds check

---

## Root Cause
`StorageService.isStatus()` called `Gossiper.instance.getEndpointStateForEndpoint(endpoint).getStatus()` without checking whether the returned `EndpointState` was `null`. Similarly, `isRpcReady()` chained `.isRpcReady()` directly on the result of `getEndpointStateForEndpoint()`. Both methods can legitimately return `null` when an endpoint has not yet been seen by gossip or has been removed — for example, during node decommission or when a replacement node has not yet fully joined. The NPE caused by the null dereference could surface in storage operations that query per-endpoint readiness.

### Before / After
```java
// BEFORE
private boolean isStatus(InetAddress endpoint, String status)
{
    return Gossiper.instance.getEndpointStateForEndpoint(endpoint).getStatus().equals(status);
}

public boolean isRpcReady(InetAddress endpoint)
{
    return MessagingService.instance().getVersion(endpoint) < MessagingService.VERSION_22 ||
            Gossiper.instance.getEndpointStateForEndpoint(endpoint).isRpcReady();
}

// AFTER
private boolean isStatus(InetAddress endpoint, String status)
{
    EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
    return state != null && state.getStatus().equals(status);
}

public boolean isRpcReady(InetAddress endpoint)
{
    if (MessagingService.instance().getVersion(endpoint) < MessagingService.VERSION_22)
        return true;
    EndpointState state = Gossiper.instance.getEndpointStateForEndpoint(endpoint);
    return state != null && state.isRpcReady();
}
```

---

## How to Spot
Any call to `Gossiper.getEndpointStateForEndpoint()` must guard against a `null` return before accessing state on the result. The gossip subsystem models endpoint knowledge as optional — an endpoint may be unknown, recently expired, or in the process of being bootstrapped. Reviewers should flag every direct method chain on the return value of `getEndpointStateForEndpoint()` that lacks a null check.

---

## Test
No test added.
