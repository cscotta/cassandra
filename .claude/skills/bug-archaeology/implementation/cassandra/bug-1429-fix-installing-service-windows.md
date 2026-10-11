# Bug 1429: Fix Installing as Service on Windows

**Commit**: 51b27f2078 · **Ticket**: CASSANDRA-8115
**Subsystem**: bin/cassandra.bat, bin/cassandra.ps1, conf/cassandra-env.ps1
**Category**: Wrong constant / default value

---

## Root Cause

The Windows service installation scripts passed JVM and Cassandra options to Apache Commons Daemon (prunsrv) as a single semicolon-delimited `++JvmOptions` string. Options that contained directory paths with spaces (e.g., `cassandra.logdir` and `cassandra.storagedir`) caused the semicolon-split parsing inside prunsrv to fail silently, so the storage and log directories were ignored at service startup. Additionally, the PowerShell script defined JVM parameter flags using named PowerShell `param()` declarations that conflicted with arbitrary `-D`/`-X` JVM arguments passed on the command line. The fix iterates over each option individually, issuing a separate `++JvmOptions` entry per token, and removes the conflicting `param()` block.

### Before / After

```bat
REM BEFORE – single delimited string, fails with spaces in paths
rem fix up java ops replace ' -' with ' ;-'
set JAVA_OPTS_DELM=%JAVA_OPTS: -=;-%
"%PRUNSRV%" //US//%SERVICE_JVM% ^
  ++JvmOptions=%JAVA_OPTS_DELM% ++JvmOptions=-DCassandra
```

```bat
REM AFTER – one ++JvmOptions token per option
:optStrip
for /F "tokens=1* delims=;" %%a in ("%tempOptions%") do (
    set JVMOPTIONS=%JVMOPTIONS% ++JvmOptions=%%a
    set tempOptions=%%b
)
if defined tempOptions goto :optStrip
%cmd% %JVMOPTIONS%
```

---

## How to Spot

Passing a multi-value configuration as a single delimited string is fragile when individual values can themselves contain the delimiter (here, spaces in directory paths defeat the `" -"` → `";-"` substitution strategy). Reviewers should ask: "can any individual option value contain the chosen delimiter?" If yes, the options must be passed one at a time or properly quoted. Windows batch and PowerShell service-installation scripts are especially prone to this because the target tool (prunsrv) has its own parsing rules that differ from the shell.

---

## Test

No test added.
