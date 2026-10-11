# Bug 0946: sstabledump Shell Script Broken Defaults and Template Divergence

**Commit**: ace7fe03d0 · **Ticket**: n/a
**Subsystem**: Tools / Shell Scripts
**Category**: Wrong constant / default value

---

## Root Cause
The `sstabledump` shell script had drifted from the standard Cassandra tool script template in several ways: the `cassandra.in.sh` search order put the per-directory file first (so a stale development copy would shadow the installed one), a `MAX_HEAP_SIZE` default was absent (causing the JVM to use its platform default, often far too large for a CLI dump tool), the `-ea` assertions flag was missing, the `-Xmx` flag was absent, and no guard existed for a missing Java executable. These collectively caused unexpected memory exhaustion, assertion errors, and confusing error messages when the script was invoked in environments where `JAVA_HOME` was unset.

### Before / After
```bash
# BEFORE — search order prioritises per-directory copy; no Java check; no heap cap
for include in "`dirname "$0"`/cassandra.in.sh" \
               "$HOME/.cassandra.in.sh" \
               /usr/share/cassandra/cassandra.in.sh \
               /usr/local/share/cassandra/cassandra.in.sh \
               /opt/cassandra/cassandra.in.sh; do
...
"$JAVA" $JAVA_AGENT -cp "$CLASSPATH" $JVM_OPTS -Dstorage-config="$CASSANDRA_CONF" \

# AFTER — system paths first; explicit Java check; default 256 M heap; -ea flag
for include in /usr/share/cassandra/cassandra.in.sh \
               /usr/local/share/cassandra/cassandra.in.sh \
               /opt/cassandra/cassandra.in.sh \
               "$HOME/.cassandra.in.sh" \
               "`dirname "$0"`/cassandra.in.sh"; do
...
if [ "x$JAVA" = "x" ]; then echo "Java executable not found" >&2; exit 1; fi
if [ "x$MAX_HEAP_SIZE" = "x" ]; then MAX_HEAP_SIZE="256M"; fi
"$JAVA" $JAVA_AGENT -ea -cp "$CLASSPATH" $JVM_OPTS -Xmx$MAX_HEAP_SIZE \
```

---

## How to Spot
Compare each tool script against the canonical `cassandra.in.sh`-loading template used by `cassandra`, `nodetool`, and `sstableutil`. Any script that has a different search order, is missing the Java-executable guard, or lacks a `MAX_HEAP_SIZE` default has drifted and should be aligned with the template.

---

## Test
No test added.
