# Bug 1560: Fix JAMM Version in Debian Package Script

**Commit**: 420c685114 · **Ticket**: n/a
**Subsystem**: debian packaging / cassandra.in.sh
**Category**: Wrong constant / default value

---

## Root Cause

The Debian package startup script hardcoded the JAMM Java agent version as `0.2.5`, but the actual JAR bundled in the distribution had been upgraded to `0.2.6`. At launch time the JVM would fail to find the agent file at the stale path, causing startup failures or silent agent omission on Debian-based installations.

### Before / After

```sh
# BEFORE
JAVA_AGENT="$JAVA_AGENT -javaagent:$CASSANDRA_HOME/lib/jamm-0.2.5.jar"
```

```sh
# AFTER
JAVA_AGENT="$JAVA_AGENT -javaagent:$CASSANDRA_HOME/lib/jamm-0.2.6.jar"
```

---

## How to Spot

When a library version is bumped, grep all packaging/launch scripts for the old version string to ensure they are updated in the same commit. A mismatch between the JAR filename on disk and the path in the launch script will always silently drop the agent or hard-fail.

---

## Test

No test added. The fix is a one-line version string correction in a shell script.
