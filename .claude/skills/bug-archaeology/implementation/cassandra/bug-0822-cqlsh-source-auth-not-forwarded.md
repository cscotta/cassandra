# Bug 0822: Authentication Lost When cqlsh SOURCE Spawns Sub-Shell

**Commit**: 50726e7780 · **Ticket**: CASSANDRA-12642
**Subsystem**: Tools / cqlsh
**Category**: State not cleaned up

---

## Root Cause
The `SOURCE` command in cqlsh creates a new `Shell` instance to execute a script file. The sub-shell constructor call did not propagate the parent shell's authentication credentials (`username`, `password`), SSL settings, connection timeout, request timeout, tracing state, or display timezone. As a result, `COPY` commands executed within a sourced script attempted to open new connections without authentication, causing auth failures against password-protected clusters.

### Before / After
```python
# BEFORE — auth_provider not passed to sub-shell
subshell = Shell(self.hostname, self.port,
                 color=self.color, encoding=self.encoding, stdin=f,
                 tty=False, use_conn=self.conn, cqlver=self.cql_version,
                 keyspace=self.current_keyspace,
                 display_timestamp_format=self.display_timestamp_format,
                 display_date_format=self.display_date_format,
                 display_nanotime_format=self.display_nanotime_format,
                 display_float_precision=self.display_float_precision,
                 max_trace_wait=self.max_trace_wait)

# AFTER — credentials and other session state forwarded
username = self.auth_provider.username if self.auth_provider else None
password = self.auth_provider.password if self.auth_provider else None
subshell = Shell(self.hostname, self.port, color=self.color,
                 username=username, password=password,
                 encoding=self.encoding, stdin=f, tty=False, use_conn=self.conn,
                 cqlver=self.cql_version, keyspace=self.current_keyspace,
                 tracing_enabled=self.tracing_enabled,
                 display_nanotime_format=self.display_nanotime_format,
                 display_timestamp_format=self.display_timestamp_format,
                 display_date_format=self.display_date_format,
                 display_float_precision=self.display_float_precision,
                 display_timezone=self.display_timezone,
                 max_trace_wait=self.max_trace_wait, ssl=self.ssl,
                 request_timeout=self.session.default_timeout,
                 connect_timeout=self.conn.connect_timeout)
```

---

## How to Spot
Whenever a "child" or "sub-shell" object is constructed from a parent, audit every parameter in the parent's constructor/initializer and ensure each piece of session state is explicitly forwarded. Absent parameters silently revert to defaults — review these by diffing the sub-shell call against the parent `Shell.__init__` signature.

---

## Test
No new test was added.
