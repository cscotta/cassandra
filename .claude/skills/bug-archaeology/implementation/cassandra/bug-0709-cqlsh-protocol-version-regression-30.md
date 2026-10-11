# Bug 0709: cqlsh Automatic Protocol Version Downgrade Regression (3.0 Backport)

**Commit**: 8f5f54fb1c · **Ticket**: CASSANDRA-13307
**Subsystem**: cqlsh / Client Drivers
**Category**: Wrong constant / default value

---

## Root Cause
This is the 3.0 backport of CASSANDRA-13307. The identical `DEFAULT_PROTOCOL_VERSION = 4` hard-coded constant was present in the 3.0 branch's cqlsh.py and was always forwarded to the Datastax driver's `Cluster` constructor, suppressing automatic protocol version negotiation. The root cause and fix are identical to the 3.x commit: remove the constant, default `protocol_version` to `None`, pass it to `Cluster` only when explicitly set by the user, and replace `DEFAULT_PROTOCOL_VERSION` in COPY tasks with `self.conn.protocol_version`.

### Before / After
```python
# BEFORE
DEFAULT_PROTOCOL_VERSION = 4
...
self.conn = Cluster(..., protocol_version=protocol_version, ...)
task = ImportTask(..., DEFAULT_PROTOCOL_VERSION, ...)

# AFTER
# constant removed
kwargs = {}
if protocol_version is not None:
    kwargs['protocol_version'] = protocol_version
self.conn = Cluster(..., **kwargs)
task = ImportTask(..., self.conn.protocol_version, ...)
```

---

## How to Spot
In rolling or mixed-version clusters, cqlsh may need to connect to nodes that do not support the highest protocol version in the codebase. Passing a hardcoded version disables negotiation. Look for any place where `protocol_version` is constructed from a module-level constant rather than from either explicit user input or the live connection's negotiated version.

---

## Test
No test added.
