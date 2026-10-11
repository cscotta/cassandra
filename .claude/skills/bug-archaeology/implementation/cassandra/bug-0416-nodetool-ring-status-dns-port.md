# Bug 0416: nodetool ring/status Shows Wrong Addresses with DNS Resolution or Port Printing

**Commit**: b61860c76e · **Ticket**: CASSANDRA-16283
**Subsystem**: tools/nodetool
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

`nodetool ring` maintained two separate code paths — one for `--print-port` mode and one for the default mode — and the default path called `probe.getLiveNodes(false)`, `getUnreachableNodes(false)`, etc., which returned addresses without ports.  The ring display then looked those non-port strings up in collections that contained port-qualified strings (or vice versa), so status fields like Up/Down/Joining/Leaving came back wrong.  Similarly, `HostStatWithPort.ipOrDns()` called `endpointWithPort.toString()`, which produces the bracketed `[host]/ip:port` form of `InetAddressAndPort`, not the plain `host:port` form expected by callers; `StorageService.getEffectiveOwnershipWithPort` had the same defect via `.toString()` instead of `.getHostAddressAndPort()`.

### Before / After

```java
// BEFORE – wrong toString form used for map key lookup
result.entrySet().stream().forEachOrdered(entry ->
    asStrings.put(entry.getKey().toString(), entry.getValue()));

// Inside printDc: live-node sets fetched without ports while display used port-qualified strings
Collection<String> liveNodes = probe.getLiveNodes(false);
String status = liveNodes.contains(endpoint) ? "Up" : ...;
```

```java
// AFTER – correct form; single unified code path always using port-qualified strings
result.entrySet().stream().forEachOrdered(entry ->
    asStrings.put(entry.getKey().getHostAddressAndPort(), entry.getValue()));

// execute() fetches all node lists once with true (port-qualified), reuses them in printDc
liveNodes = probe.getLiveNodes(true);
```

---

## How to Spot

Whenever a nodetool command builds a status map from one set of strings and looks up entries in another set of strings, verify that both sets use the same address representation (plain IP, IP+port, or DNS name).  A tell-tale sign is a boolean flag (`printPort`, `resolveIp`) that is passed inconsistently to the various `probe.get*Nodes()` calls, or use of `InetAddressAndPort.toString()` (which includes the class name and brackets) instead of `getHostAddressAndPort()`.

---

## Test

New `RingTest` and `StatusTest` unit tests exercise ring and status output with both port-printing and DNS-resolution flags enabled and assert that each address appears correctly in the formatted output.
