# Bug 0925: Cassandra Fails to Launch on Windows When Installation Path Contains Spaces

**Commit**: 96c53e0a5e · **Ticket**: CASSANDRA-11515
**Subsystem**: Packaging / Windows launch scripts
**Category**: Wrong constant / default value

---

## Root Cause
`cassandra-env.ps1` built the `-Xloggc:` JVM option by directly interpolating `$env:CASSANDRA_HOME` into the option string without quoting. When `CASSANDRA_HOME` contained one or more space characters (e.g. `C:\Program Files\Cassandra`), the JVM received a split argument and could not find the log file path, causing startup to fail. The fix wraps the path with escaped double-quotes so that the PowerShell string expansion preserves the full path as a single JVM argument even when spaces are present.

### Before / After
```powershell
# BEFORE — unquoted path breaks on spaces
$env:JVM_OPTS="$env:JVM_OPTS -Xloggc:$env:CASSANDRA_HOME/logs/gc.log"

# AFTER — path quoted so spaces are preserved
$env:JVM_OPTS="$env:JVM_OPTS -Xloggc:""$env:CASSANDRA_HOME/logs/gc.log"""
```

---

## How to Spot
Any place in a shell or PowerShell script where an environment variable that may contain a path is interpolated directly into a command-line argument without quoting is a potential space-in-path bug. During review of launch scripts, search for `-X` JVM flags or file-path arguments that do not wrap the variable reference in quotes. This is especially important on Windows where `Program Files` and user home directories commonly contain spaces.

---

## Test
No test added.
