# Bug 0390: Debian Init Script Fails to Stop Cassandra on Buster

**Commit**: 99b6095ecc · **Ticket**: CASSANDRA-15770
**Subsystem**: debian/init
**Category**: Wrong constant / default value

---

## Root Cause

The `do_stop()` function in the Debian SysV init script used `start-stop-daemon -K -p "$PIDFILE"` to stop the Cassandra process. On Debian Buster (and later), `start-stop-daemon` requires a matching condition that unambiguously identifies the process; relying solely on the PID file (`-p`) without also specifying the owning user (`-u cassandra`) causes the stop command to fail silently when the PID file exists but the process owner check does not match. Adding `-u cassandra` makes the stop condition explicit and reliable.

### Before / After

```sh
# BEFORE
start-stop-daemon -K -p "$PIDFILE" -R TERM/30/KILL/5 >/dev/null
```

```sh
# AFTER
start-stop-daemon -K -u cassandra -p "$PIDFILE" -R TERM/30/KILL/5 >/dev/null
```

---

## How to Spot

When a stop/kill target uses only a PID file without also constraining on the process owner or executable name, a behavior change in the `start-stop-daemon` version shipped by a new OS release can silently break the stop path. Review init scripts whenever upgrading the target OS to confirm that all matching options (`-p`, `-u`, `-n`, `-x`) together uniquely and reliably identify the target process under the new runtime.

---

## Test

No test added.
