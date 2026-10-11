# Bug 1596: Fix Heap Size Calculation on Windows

**Commit**: b29d882df8 · **Ticket**: CASSANDRA-7352
**Subsystem**: conf/cassandra-env.ps1
**Category**: Wrong constant / default value

---

## Root Cause

The PowerShell startup script `cassandra-env.ps1` performed heap size calculations using raw division of integer quantities (total memory bytes, half-memory, quarter-memory, and desired young-gen size). In PowerShell, dividing integers can yield floating-point results, and when those float values were later used in string comparisons or arithmetic with `-gt` comparisons, the behavior was unpredictable—potentially producing fractional MB values that could not be passed cleanly to JVM `-Xmx`/`-Xmn` flags.

### Before / After

```powershell
# BEFORE: raw division produces floating-point values
$memoryMB = $memory / (1024*1024)
$halfMem = $memoryMB / 2
$quarterMem = $halfMem / 2
$desiredYG = $maxHeapMB / 4
```

```powershell
# AFTER: truncate to integer after each division
$memoryMB = [Math]::Truncate($memory / (1024*1024))
$halfMem = [Math]::Truncate($memoryMB / 2)
$quarterMem = [Math]::Truncate($halfMem / 2)
$desiredYG = [Math]::Truncate($maxHeapMB / 4)
```

---

## How to Spot

In PowerShell (unlike Bash arithmetic), division of integers yields doubles. Any script that divides a memory or size quantity and then uses the result as a JVM argument or in integer comparisons must explicitly truncate or floor the result. Review all arithmetic in `.ps1` scripts for missing `[Math]::Truncate` or `[int]` casts.

---

## Test

No test added.
