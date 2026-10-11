# Bug 1053: JVM_OPTS Duplicated in Debian Package init Script

**Commit**: 9d8436f922 · **Ticket**: CASSANDRA-8821
**Subsystem**: Packaging / Debian
**Category**: Wrong constant / default value

---

## Root Cause
The Debian `init` script sourced `cassandra-env.sh` itself (which sets `JVM_OPTS`) and then exported `JVM_OPTS` before calling the Cassandra daemon. The daemon start script also sources `cassandra-env.sh` internally. As a result `JVM_OPTS` was populated twice, and any options that `cassandra-env.sh` appended conditionally could end up duplicated in the actual JVM invocation, leading to unexpected behaviour (e.g. doubled GC flags, doubled heap settings).

The fix removes the redundant sourcing block entirely from `debian/init`. The daemon launcher is responsible for setting up its own environment; the init script does not need to replicate that step.

### Before / After
```bash
# BEFORE – debian/init sourced cassandra-env.sh and re-exported JVM_OPTS
. /etc/cassandra/cassandra-env.sh

if [ -z "$JVM_OPTS" ]; then
    echo "Initialization failed; \$JVM_OPTS not set!" >&2
    exit 3
fi

export JVM_OPTS

# AFTER – the block is removed; JVM_OPTS is set solely by the daemon launcher
# (no corresponding code – the lines are simply deleted)
```

---

## How to Spot
When a service init/wrapper script and the application launcher both source the same environment-setup script, any variable that script appends to (rather than assigns) will accumulate duplicate values. Review any `export` of `JVM_OPTS` or similar accumulating variables in init scripts and verify there is exactly one point of assembly.

---

## Test
No test added (packaging change).
