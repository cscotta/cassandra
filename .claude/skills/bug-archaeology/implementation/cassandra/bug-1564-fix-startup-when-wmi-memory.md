# Bug 1564: Fix Startup When WMI Memory Query Fails on Windows

**Commit**: c29e48d7ac · **Ticket**: CASSANDRA-7505
**Subsystem**: conf / cassandra-env.ps1 (Windows startup)
**Category**: Missing null / bounds check

---

## Root Cause

On some Windows environments `Get-WMIObject -class win32_physicalmemory` returns `$null` (e.g. when run inside certain virtualisation platforms or when WMI is degraded). The PowerShell startup script assumed the result was always a valid object and immediately piped it to `Measure-Object`, which throws a terminating error when the input is `$null`. This prevented Cassandra from starting at all on affected Windows hosts.

### Before / After

```powershell
# BEFORE – no null guard; crashes when WMI returns nothing
$memObject = Get-WMIObject -class win32_physicalmemory
$memory = ($memObject | Measure-Object Capacity -Sum).sum
```

```powershell
# AFTER – fall back to safe defaults when WMI returns null
$memObject = Get-WMIObject -class win32_physicalmemory
if ($memObject -eq $null)
{
    echo "WARNING!  Could not determine system memory.  Defaulting to 2G heap, 512M newgen. ..."
    $env:MAX_HEAP_SIZE = "2048M"
    $env:HEAP_NEWSIZE = "512M"
    return
}
$memory = ($memObject | Measure-Object Capacity -Sum).sum
```

---

## How to Spot

WMI queries can return `$null` on many Windows configurations. Any PowerShell script that calls `Get-WMIObject` (or its successor `Get-CimInstance`) must null-check the result before using it. This pattern should be enforced as a lint rule for Windows configuration scripts.

---

## Test

No test added. The fix is a defensive null check in a PowerShell configuration script.
