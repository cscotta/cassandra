# Bug 0494: cqlsh Shell Script Arguments Evaluated via Word Splitting

**Commit**: 781e4862fa · **Ticket**: CASSANDRA-15660
**Subsystem**: bin/cqlsh (shell script)
**Category**: Logic error in condition

---

## Root Cause

The original `cqlsh` wrapper script accumulated extra arguments in a single string variable `PARAMS` and passed it unquoted to the `exec` call, relying on shell word-splitting to re-expand the arguments. This is a classic shell injection risk: any argument containing spaces, glob characters, or shell metacharacters would be re-parsed and evaluated by the shell rather than passed literally. The fix rewrites argument handling using `set -- "$@" "$arg"` to accumulate arguments in the positional parameter array and passes `"$@"` (properly quoted) to `exec`, so each argument is preserved verbatim.

### Before / After

```sh
# BEFORE — concatenates args into a string, then passes unquoted
PARAMS="$PARAMS $1"
...
exec "$interpreter" "$path/cqlsh.py" $PARAMS   # word-split, evaluated
```

```sh
# AFTER — accumulates in positional params, passes quoted
set -- "$@" "$arg"
...
exec "$interpreter" "$path/cqlsh.py" "$@"      # each arg preserved
```

---

## How to Spot

Any shell script that stores multiple arguments in a single variable and later uses `$variable` (unquoted) in a command is vulnerable to word-splitting. Review wrapper scripts for pattern `PARAMS="$PARAMS $1"` followed by `exec ... $PARAMS` and replace with the `"$@"` accumulation idiom. Shellcheck would flag the unquoted variable expansion.

---

## Test

No test added.
