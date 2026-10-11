# Bug 1427: Fix Regression in cassandra.ps1 Introduced by CASSANDRA-8115

**Commit**: 7cf3f19be0 · **Ticket**: CASSANDRA-8115
**Subsystem**: bin/cassandra.ps1
**Category**: Logic error in condition

---

## Root Cause

The original Windows service installation patch (CASSANDRA-8115) added argument-parsing logic that extracted `-D` JVM options from the command line, but the extraction code stripped the leading `-D` prefix and reattached it, effectively double-encoding options that started with `-X` (e.g., `-Xmx`, `-Xms`). Additionally, the main argument-parsing switch statement did not skip `-D`/`-X` entries before trying to match them against named flags, causing a PowerShell error when an unknown argument reached the switch. The fix passed `-D` and `-X` arguments through verbatim and added a guard to skip them in the switch.

### Before / After

```powershell
# BEFORE – strips prefix and reattaches, drops -X args entirely
if ($script:args[$i].Substring(0,2) -eq "-D")
{
    $param = $script:args[$i].Substring(2)
    $env:JVM_OPTS = "$env:JVM_OPTS -D$param"  # -X args silently ignored
}
```

```powershell
# AFTER – passes arg verbatim, handles both -D and -X
if ($script:args[$i].StartsWith("-D") -Or $script:args[$i].StartsWith("-X"))
{
    $env:JVM_OPTS = "$env:JVM_OPTS " + $script:args[$i]
}
```

---

## How to Spot

String prefix stripping followed by re-prefixing (`Substring(2)` + `"-D$param"`) is fragile: it silently drops any prefix that does not match the hardcoded string. Whenever command-line argument forwarding is implemented, reviewers should verify that the forwarding is prefix-agnostic (pass the whole token) and that the consuming loop guards against argument families it is not designed to handle before they reach a default-case switch.

---

## Test

No test added.
