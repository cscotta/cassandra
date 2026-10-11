# Bug 0121: CLASSPATH Broken in cassandra.in.sh for Development Builds

**Commit**: d6f623b308 · **Ticket**: CASSANDRA-20257
**Subsystem**: tools/bin
**Category**: Logic error in condition

---

## Root Cause

The `tools/bin/cassandra.in.sh` shell script built the `CLASSPATH` by globbing `build/apache-cassandra*.jar`. With a plain `ls -1` the glob silently matched multiple JARs when `javadoc` and `sources` artifact JARs were present alongside the main JAR (a common state for development builds after `ant jar`), causing an incorrectly formed `CLASSPATH` string that concatenated several paths with newlines rather than colons. Additionally there was no guard against the glob matching zero JARs (e.g. before the first build), which would pass an empty/literal glob string into the classpath.

### Before / After

```sh
# BEFORE – no filtering of javadoc/sources JARs; no check for 0 or >1 matches
cassandra_bin=`ls -1 $CASSANDRA_HOME/build/apache-cassandra*.jar`
cassandra_bin="$cassandra_bin:$CASSANDRA_HOME/build/classes/stress:..."
CLASSPATH="$CLASSPATH:$cassandra_bin"
```

```sh
# AFTER – count filtered JARs; error on ambiguity; skip when none present
jars_cnt="`ls -1 $CASSANDRA_HOME/build/apache-cassandra*.jar | grep -v 'javadoc.jar' | grep -v 'sources.jar' | wc -l | xargs echo`"
if [ "$jars_cnt" -gt 1 ]; then
    echo "There are JAR artifacts for multiple versions..." 1>&2
    exit 1
fi
if [ "$jars_cnt" = "1" ]; then
    cassandra_bin="`ls -1 ... | grep -v javadoc | grep -v sources`"
    CLASSPATH="$CLASSPATH:$cassandra_bin"
fi
```

---

## How to Spot

Shell scripts that use unfiltered globs to locate a single artifact are fragile: build systems often emit companion JARs (javadoc, sources, test) whose names match the same pattern. Any script that expects exactly one result from a glob should filter unintended variants and guard against zero or multiple matches with an explicit count check before using the result.

---

## Test

No automated test added; the fix is verified manually by inspecting CLASSPATH output after a development build.
