**Commit**: 9bbf0950f9 · **Ticket**: KAFKA-17387
**Subsystem**: tools (VerifiableConsumer)
**Category**: Wrong constant / default value

**Root cause**
`VerifiableConsumer` still accepted the deprecated `--broker-list` option as an alias for `--bootstrap-server`. Keeping the old flag as an accepted option in a tool shipped with Kafka 4.0 implied it was still valid, creating confusion and test scripts that would silently use the wrong option name with future versions. The Python test service was also updated to pass `--bootstrap-server` for v4.0+ nodes and `--broker-list` only for older versions.

**Before/After**
```java
// Before
parser.accepts("broker-list").withRequiredArg();
// Python: always used --broker-list

// After – broker-list removed from Java tool
// Python: version-conditional
if node_version >= "4.0":
    cmd = "--bootstrap-server ..."
else:
    cmd = "--broker-list ..."
```

**How to spot**
When removing a deprecated CLI option from a tool, audit all test harness scripts (Python ducktape services, shell scripts) that invoke the tool; they must be updated concurrently to avoid breakage in cross-version E2E tests.

**Test**
No test added.
