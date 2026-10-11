# Bug 1057: Windows Startup Aborts When Both MAX_HEAP_SIZE and HEAP_NEWSIZE Are Set

**Commit**: fa909cc420 · **Ticket**: CASSANDRA-10553
**Subsystem**: Startup / Configuration (cassandra-env.ps1)
**Category**: Logic error in condition

---

## Root Cause
The PowerShell startup script `cassandra-env.ps1` reached the validation check `($env:MAX_HEAP_SIZE -and !$env:HEAP_NEWSIZE) -or (!$env:MAX_HEAP_SIZE -and $env:HEAP_NEWSIZE)` without first short-circuiting when both variables were already set. The "both set" state is the fully-configured, valid case, but the code fell through to the auto-calculation logic instead of returning early. The fix adds an explicit early `return` at the top of `CalculateHeapSizes` when both `MAX_HEAP_SIZE` and `HEAP_NEWSIZE` are already defined, so the auto-sizing block is skipped entirely when the operator has pre-configured both values.

### Before / After
```powershell
# BEFORE — no early exit; fell into the mismatch-check then auto-calculation
if (($env:MAX_HEAP_SIZE -and !$env:HEAP_NEWSIZE) -or (!$env:MAX_HEAP_SIZE -and $env:HEAP_NEWSIZE))
{
    echo "Please set or unset MAX_HEAP_SIZE and HEAP_NEWSIZE in pairs.  Aborting startup."
    ...
}

# AFTER — early return added before the mismatch check
if ($env:MAX_HEAP_SIZE -and $env:HEAP_NEWSIZE)
{
    return
}
if (($env:MAX_HEAP_SIZE -and !$env:HEAP_NEWSIZE) -or (!$env:MAX_HEAP_SIZE -and $env:HEAP_NEWSIZE))
{
    ...
}
```

---

## How to Spot
Whenever a configuration-loading function distinguishes "both set", "one set", and "neither set" cases, the "both set" case must be the first branch tested. If it is omitted or tested last, the function can incorrectly treat a valid fully-specified configuration as a partial configuration or continue into unnecessary auto-calculation code.

---

## Test
No test added.
