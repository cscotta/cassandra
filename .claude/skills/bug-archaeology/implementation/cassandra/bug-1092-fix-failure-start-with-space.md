# Bug 1092: Fix Failure to Start with Space in Directory Name on Windows

**Commit**: e1fc0e0671 · **Ticket**: CASSANDRA-10239
**Subsystem**: bin/cassandra.bat, conf/cassandra-env.ps1
**Category**: Wrong constant / default value

---

## Root Cause

On Windows, paths containing spaces must be quoted when passed as JVM system-property values or PowerShell string arguments. `cassandra.bat` passed `-Djava.library.path=` followed by an unquoted `%CASSANDRA_HOME%\lib\sigar-bin`, so the JVM saw a truncated path whenever `CASSANDRA_HOME` contained a space. Similarly, `cassandra-env.ps1` double-quoted the entire `$env:CLASSPATH` variable inside `SetJsr223Env`, causing it to expand to a quoted-string literal rather than the raw path, which broke classpath assembly. Both scripts failed silently at startup rather than at build time.

### Before / After

```bat
REM BEFORE (cassandra.bat)
-Djava.library.path=%CASSANDRA_HOME%\lib\sigar-bin^
```

```bat
REM AFTER (cassandra.bat)
-Djava.library.path="%CASSANDRA_HOME%\lib\sigar-bin"^
```

```powershell
# BEFORE (cassandra-env.ps1)
$cp = """$env:CLASSPATH"""
```

```powershell
# AFTER (cassandra-env.ps1)
$cp = $env:CLASSPATH
```

---

## How to Spot

Whenever a shell or batch script embeds an environment variable that represents a filesystem path into a command line without surrounding quotes, it will break for any installation path that contains whitespace. Review every `%VAR%` or `$env:VAR` expansion used in a JVM argument or classpath assignment and ensure it is quoted. PowerShell double-quoting of an already-string variable is equally suspect: the extra quotes become part of the string value.

---

## Test

No test added.
