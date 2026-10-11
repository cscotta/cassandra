# Bug 1947: nodetool getendpoints fails when partition key contains a space

**Commit**: ddb501df40 · **Ticket**: CASSANDRA-4551
**Subsystem**: bin/nodetool
**Category**: Wrong serialization / deserialization

---

## Root Cause

The `nodetool` shell script passed command-line arguments to the JVM using the unquoted `$@` form. In POSIX shells, bare `$@` undergoes word-splitting, so a partition key containing a space (e.g., `"hello world"`) would be split into two separate arguments before reaching the Java `NodeCmd` main method. The fix uses the standard `${1+"$@"}` idiom, which preserves each argument as a single token regardless of embedded whitespace.

### Before / After

```sh
# BEFORE — word-splitting breaks keys with spaces
org.apache.cassandra.tools.NodeCmd $@
```

```sh
# AFTER — quoted form preserves whitespace in arguments
org.apache.cassandra.tools.NodeCmd ${1+"$@"}
```

---

## How to Spot

Any shell script that forwards positional parameters to a Java (or other) command using bare `$@` or `$*` will silently split arguments containing spaces or other IFS characters. Always use `"$@"` (double-quoted) when forwarding arguments, or the equivalent `${1+"$@"}` to handle the zero-argument case portably.

---

## Test

No test added.
