# Bug 0700: RedHat Init Script Stop Action Checks Wrong Status

**Commit**: 868be9c283 · **Ticket**: CASSANDRA-13435
**Subsystem**: redhat/cassandra (init script)
**Category**: Logic error in condition

---

## Root Cause

The stop action in the RedHat/CentOS SysV init script polled for process termination by calling `$0 status` (i.e., recursively re-invoking the init script itself) and comparing its stdout to the string `"$NAME is stopped"`. This approach was fragile: any change in status message wording, locale output, or string-comparison construct (`[[ ]]` vs `[ ]`) would silently break the check. More critically, `$0 status` delegates to the same script's `status` case, which may itself have bugs — and the poll loop `for t in \`seq 40\`; do $0 status ... && sleep 0.5 || break; done` inverted the intended logic (the loop broke on failure, not on success). The fix replaces recursive self-invocation with the standard `status -p $pid_file cassandra` utility which returns exit code 3 when the process is not running, providing a stable, locale-independent check.

### Before / After

```bash
# BEFORE
for t in `seq 40`; do $0 status > /dev/null 2>&1 && sleep 0.5 || break; done
STATUS=`$0 status`
if [[ $STATUS == "$NAME is stopped" ]]; then
    echo "OK"
else
    echo "ERROR: could not stop $NAME:  $STATUS"
    exit 1
fi
```

```bash
# AFTER
for t in `seq 40`; do
    status -p $pid_file cassandra > /dev/null 2>&1
    retval=$?
    if [ $retval -eq 3 ]; then echo "OK"; exit 0
    else sleep 0.5; fi
done
status -p $pid_file cassandra > /dev/null 2>&1
retval=$?
if [ $retval -eq 3 ]; then echo "OK"
else echo "ERROR: could not stop $NAME"; exit 1; fi
```

---

## How to Spot

Init scripts that verify process termination by comparing human-readable status strings are fragile. The standard POSIX approach is to use `status -p <pidfile>` or similar and inspect the exit code; exit code 3 conventionally means "not running". A reviewer should replace any string comparison against `$0 status` output with a direct exit-code check on the pid-file-aware status utility.

---

## Test

No test added (init script functionality).
