# Bug 0708: cqlsh Automatic Protocol Version Downgrade Regression

**Commit**: 9c54d02f73 · **Ticket**: CASSANDRA-13307
**Subsystem**: cqlsh / Client Drivers
**Category**: Wrong constant / default value

---

## Root Cause
A `DEFAULT_PROTOCOL_VERSION = 4` constant was hard-coded in cqlsh.py and passed unconditionally to the Datastax driver's `Cluster` constructor. When cqlsh connected to an older Cassandra node that did not support protocol v4, the driver's built-in automatic downgrade logic was disabled because an explicit version had been supplied. As a result, the connection failed instead of negotiating down to a supported version. The fix removes the hard-coded default, makes `protocol_version` default to `None`, and only passes `protocol_version` to the `Cluster` constructor when the user explicitly supplies `--protocol-version`. COPY tasks, which also passed the constant directly, are updated to use the version actually negotiated by the live connection (`self.conn.protocol_version`).

### Before / After
```python
# BEFORE
DEFAULT_PROTOCOL_VERSION = 4
...
self.conn = Cluster(contact_points=(self.hostname,), port=self.port,
                    cql_version=cqlver,
                    protocol_version=protocol_version,   # always 4
                    ...)
...
task = ImportTask(..., DEFAULT_PROTOCOL_VERSION, ...)
task = ExportTask(..., DEFAULT_PROTOCOL_VERSION, ...)

# AFTER
# constant removed; protocol_version defaults to None
kwargs = {}
if protocol_version is not None:
    kwargs['protocol_version'] = protocol_version
self.conn = Cluster(contact_points=(self.hostname,), port=self.port,
                    cql_version=cqlver,
                    ...
                    **kwargs)          # omitted when None, allowing auto-negotiate
...
task = ImportTask(..., self.conn.protocol_version, ...)
task = ExportTask(..., self.conn.protocol_version, ...)
```

---

## How to Spot
Whenever a driver wrapper passes an explicit protocol version to the underlying driver library, check whether that value is derived from user input or is a hard-coded default. A hard-coded version disables the driver's negotiation logic and will break connections to nodes running older protocol versions. Also verify that downstream operations (COPY, tracing) use the negotiated version from the live connection rather than a startup default.

---

## Test
No test added in this commit.
