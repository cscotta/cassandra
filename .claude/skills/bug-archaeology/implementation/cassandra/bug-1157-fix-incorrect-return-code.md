# Bug 1157: SSTableVerify.bat Does Not Propagate Non-Zero Exit Code to Caller

**Commit**: 31c1958e2c · **Ticket**: CASSANDRA-9941
**Subsystem**: bin/sstableverify.bat
**Category**: Wrong constant / default value

---

## Root Cause

`sstableverify.bat` used `ENDLOCAL` without capturing `ERRORLEVEL` before the local variable scope was closed. `ENDLOCAL` resets all local variables, including `ERRORLEVEL`, to their values from the outer scope, so any error status set inside the script was silently discarded. Additionally, the `:err` label set no `ERRORLEVEL` before jumping to `:finally`, meaning a missing-JAVA_HOME error also exited with code 0. The fix captures `%ERRORLEVEL%` into a temporary variable before `ENDLOCAL` and uses a `call :returncode` trampoline — a standard Windows batch idiom — to re-apply the saved code after the scope is restored. A companion change in `cassandra.in.bat` also corrects `CLASSPATH` to use `%CASSANDRA_CONF%` rather than the hard-coded `%CASSANDRA_HOME%\conf`.

### Before / After

```bat
REM BEFORE – ERRORLEVEL lost after ENDLOCAL
:err
echo JAVA_HOME environment variable must be set!
pause

:finally
ENDLOCAL
```

```bat
REM AFTER – ERRORLEVEL captured before ENDLOCAL and re-applied via trampoline
:err
echo JAVA_HOME environment variable must be set!
set ERRORLEVEL=1
pause

:finally
ENDLOCAL & set RC=%ERRORLEVEL%
goto :exit_with_code

:returncode
exit /B %RC%

:exit_with_code
call :returncode %RC%
```

---

## How to Spot

Windows batch scripts that use `SETLOCAL`/`ENDLOCAL` must capture `%ERRORLEVEL%` into a plain variable before calling `ENDLOCAL`, because `ENDLOCAL` discards all local variable mutations. Any `.bat` script that calls `ENDLOCAL` as the last statement before exit without a prior `set RC=%ERRORLEVEL%` will always exit with the caller's original `ERRORLEVEL`. Reviewers should also verify that every error path explicitly `set ERRORLEVEL=<non-zero>` since Windows does not always propagate failures automatically.

---

## Test

No test added.
