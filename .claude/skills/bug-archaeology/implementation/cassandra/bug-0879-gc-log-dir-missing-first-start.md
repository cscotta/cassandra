# Bug 0879: GC Log Directory Missing on First Start

**Commit**: 3a61c10659 · **Ticket**: CASSANDRA-11949
**Subsystem**: Startup / JVM configuration scripts
**Category**: Missing null / bounds check

---

## Root Cause
`cassandra-env.sh` (and its PowerShell equivalent `cassandra-env.ps1`) passed `-Xloggc:${CASSANDRA_HOME}/logs/gc.log` to the JVM without first ensuring the `logs/` directory existed. On a fresh installation or after the logs directory was removed, the JVM failed to open the GC log file and either silently dropped GC logging or aborted startup. The fix adds `mkdir -p ${CASSANDRA_HOME}/logs/` (and the PowerShell `New-Item -Force -ItemType directory` equivalent) immediately before the `-Xloggc` option is set.

### Before / After
```bash
# BEFORE — logs/ may not exist; JVM cannot open gc.log
JVM_OPTS="$JVM_OPTS -Xloggc:${CASSANDRA_HOME}/logs/gc.log"

# AFTER — directory created before JVM is told to write there
mkdir -p ${CASSANDRA_HOME}/logs/
JVM_OPTS="$JVM_OPTS -Xloggc:${CASSANDRA_HOME}/logs/gc.log"
```

---

## How to Spot
Any script that writes to a file path should ensure the parent directory exists before passing that path to a subprocess. Startup scripts that configure JVM options pointing to log files are especially prone to this because the logs directory is often absent on a clean install or in a container environment. Review startup scripts for `-Xloggc`, `-Xlog:gc:file=`, or similar flags and confirm a `mkdir -p` precedes them.

---

## Test
No automated test added; verified manually by running a fresh node.
