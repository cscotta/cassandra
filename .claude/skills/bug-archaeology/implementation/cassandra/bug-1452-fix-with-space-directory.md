# Bug 1452: stop-server.bat fails when CASSANDRA_HOME contains a space

**Commit**: cb8dc4eac0 · **Ticket**: CASSANDRA-8044
**Subsystem**: bin/stop-server.bat
**Category**: Wrong constant / default value

---

## Root Cause

The `start /B powershell /file` invocation in `stop-server.bat` passed the path to `stop-server.ps1` without quotes. When `CASSANDRA_HOME` contains a directory name with an embedded space (e.g., `C:\Program Files\cassandra`), the Windows shell splits the unquoted path at the space, causing PowerShell to receive a truncated filename and fail to locate the script. The fix is to wrap the expanded path in double-quotes so it is treated as a single token regardless of spaces in the installation path.

### Before / After

```bat
REM BEFORE — unquoted path breaks on spaces
start /B powershell /file %CASSANDRA_HOME%/bin/stop-server.ps1 -batchpid %PID% %*
```

```bat
REM AFTER — quoted path survives spaces
start /B powershell /file "%CASSANDRA_HOME%/bin/stop-server.ps1" -batchpid %PID% %*
```

---

## How to Spot

Any Windows batch file that expands an environment variable into a command argument without surrounding double-quotes is a latent space-in-path bug. Review every `%VAR%` token in `start`, `call`, and direct invocations to ensure paths are quoted. The pattern `%SOME_HOME%/some/path` (no quotes) is the canonical signal.

---

## Test

No test added.
