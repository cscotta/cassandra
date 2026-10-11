# Bug 1217: IPv6 Address Parsed Incorrectly on Windows Startup

**Commit**: c1702b0b3c · **Ticket**: CASSANDRA-9567
**Subsystem**: bin/cassandra.ps1
**Category**: Logic error in condition

---

## Root Cause

The Windows PowerShell startup script parses `cassandra.yaml` to extract `listen_address` and `rpc_address` values for port-availability checking. When splitting a YAML line such as `listen_address: ::1`, the script split on the bare `:` character rather than the YAML key-value separator `": "`. Because an IPv6 address contains multiple colons, splitting on `:` alone fragments the address into many tokens; only `$args[1]` is kept, producing a partial address rather than the full IPv6 literal. IPv4 addresses were silently unaffected because they contain no colons beyond the separator.

### Before / After

```powershell
# BEFORE
$args = $line -Split ":"
$listenAddress = $args[1] -replace " ", ""
```

```powershell
# AFTER
$args = $line -Split ": "
$listenAddress = $args[1] -replace " ", ""
```

---

## How to Spot

Whenever a script or configuration-file parser splits on a single-character delimiter that can legally appear inside the value being parsed (colons in IPv6, commas in quoted CSV, slashes in URIs), the result will be wrong for any value containing that character. Review every `-Split ":"` (or equivalent) that operates on YAML, properties, or configuration lines to ensure it splits on the full key-value separator — typically `": "` — not just the structural character itself.

---

## Test

No test added.
