# Bug 1260: JVM_EXTRA_OPTS Not Expanded in Windows PowerShell Startup Script

**Commit**: a431d846c1 · **Ticket**: CASSANDRA-9335
**Subsystem**: conf/cassandra-env.ps1
**Category**: Wrong constant / default value

---

## Root Cause

In the PowerShell startup script `cassandra-env.ps1`, the line that appends extra JVM options used `$JVM_EXTRA_OPTS` (a bare variable) instead of `$env:JVM_EXTRA_OPTS` (an environment variable). In PowerShell, `$JVM_EXTRA_OPTS` is a script-scope local variable that is never set, so it always expands to an empty string, silently ignoring any `JVM_EXTRA_OPTS` value set in the calling environment. All other JVM option references in the same script correctly use the `$env:` prefix.

### Before / After

```powershell
# BEFORE — bare variable, always empty; user-set env var is silently ignored
$env:JVM_OPTS="$env:JVM_OPTS $JVM_EXTRA_OPTS"
```

```powershell
# AFTER — correct env: scope; picks up the caller's JVM_EXTRA_OPTS value
$env:JVM_OPTS="$env:JVM_OPTS $env:JVM_EXTRA_OPTS"
```

---

## How to Spot

In PowerShell scripts, local variables (`$foo`) and environment variables (`$env:foo`) are distinct namespaces. Any reference to a variable that is never assigned in the script itself should use `$env:` to read from the process environment. A reviewer can grep the script for bare `$VARIABLE_NAME` references and cross-check whether each one is assigned locally; if not, the `$env:` prefix is almost certainly missing.

---

## Test

No test added.
