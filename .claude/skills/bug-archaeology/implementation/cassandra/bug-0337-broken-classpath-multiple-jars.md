# Bug 0337: Broken Classpath When Multiple JARs in Build Directory

**Commit**: aaffb3b53e · **Ticket**: CASSANDRA-17129
**Subsystem**: bin/cassandra.in.sh
**Category**: Logic error in condition

---

## Root Cause

`cassandra.in.sh` used a bare `ls -1 $CASSANDRA_HOME/build/apache-cassandra*.jar` to select the JAR to add to `CLASSPATH`. When the build directory contained more than one matching JAR (e.g., after building multiple versions without cleaning), the `ls` output contained multiple newline-separated paths, producing a multi-token value that was appended verbatim to `CLASSPATH`. The resulting classpath string was syntactically broken and caused Cassandra to fail to start. The fix counts matching non-javadoc/non-sources JARs and exits with a clear error if more than one is found.

### Before / After

```sh
# BEFORE – silently builds a broken classpath with multiple paths
cassandra_bin=`ls -1 $CASSANDRA_HOME/build/apache-cassandra*.jar`
CLASSPATH="$CLASSPATH:$cassandra_bin"
```

```sh
# AFTER – detect and reject the ambiguous state up front
jars_cnt="`ls -1 $CASSANDRA_HOME/build/apache-cassandra*.jar | grep -v 'javadoc.jar' | grep -v 'sources.jar' | wc -l | xargs echo`"
if [ "$jars_cnt" -gt 1 ]; then
    echo "There are JAR artifacts for multiple versions in the $dir directory. Please clean the project with 'ant realclean' and build it again." 1>&2
    exit 1
fi
if [ "$jars_cnt" = "1" ]; then
    cassandra_bin="`ls -1 $CASSANDRA_HOME/build/apache-cassandra*.jar | grep -v javadoc | grep -v sources`"
    CLASSPATH="$CLASSPATH:$cassandra_bin"
fi
```

---

## How to Spot

Shell scripts that use command substitution to capture multi-line output and then embed it unquoted into a colon-separated path variable are vulnerable to silent breakage when the command returns more than one line. The correct defensive pattern is to count results first and fail with an actionable error rather than silently constructing a malformed value.

---

## Test

No test added; the fix is in a shell startup script where automated testing is impractical.
