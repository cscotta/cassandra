# Bug 1008: cassandra start silently succeeds when run as root

**Commit**: 425dc5f7dc · **Ticket**: CASSANDRA-8142
**Subsystem**: Startup / shell scripts
**Category**: Logic error in condition

---

## Root Cause
The `bin/cassandra` startup script accepted unknown trailing arguments (such as `start`) without error because the `getopt` parsing loop treated them as no-ops: after the `--` sentinel the script simply `break`s without checking whether `$*` is empty.  Anything after `--` was silently ignored, so `cassandra start` would launch a full daemon.  At the same time there was no guard against running Cassandra as the root user or root group, which is operationally dangerous.  The fix adds an explicit check that `$*` is empty after option parsing and exits with an error if it is not, and adds a `-R` flag to allow deliberate root execution while defaulting to rejection.

### Before / After
```bash
# BEFORE – args= uses getopt without 'R'; after -- break is unconditional
args=`getopt vfhp:bD:H:E: "$@"`
...
        --)
            shift
            break
        ;;
        *)
            echo "Error parsing arguments!" >&2
            exit 1
        ;;

# AFTER – 'R' added to getopt; trailing args cause hard exit; root guard added
args=`getopt vRfhp:bD:H:E: "$@"`
...
        -R)
            allow_root="yes"
            shift
        ;;
        --)
            shift
            if [ "x$*" != "x" ] ; then
                echo "Error parsing arguments! Unknown argument \"$*\"" >&2
                exit 1
            fi
            break
        ;;

# Root/group guard added after option loop:
if [ "x$allow_root" != "xyes" ] ; then
    if [ "`id -u`" = "1" ] || [ "`id -g`" = "0" ] ; then
        echo "Running Cassandra as root user or group is not recommended ..."
        exit 1
    fi
fi
```

---

## How to Spot
Any shell-script option parser that calls `getopt` and then breaks unconditionally after `--` will silently swallow non-option arguments.  Look for a `shift; break` inside the `--` arm of a `while/case` loop without a guard on `$*`.  Similarly, scripts that launch long-running daemons should always check `id -u` / `id -g` or equivalent to reject root execution by default.

---

## Test
No automated test added (shell-script behaviour); the change is validated by manual invocation.
