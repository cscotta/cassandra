# Bug 0203: RPM Startup Script Fails to Select jvm17 Options on JDK 17

**Commit**: 17d0eb138c · **Ticket**: CASSANDRA-18817
**Subsystem**: Packaging / startup scripts (redhat/cassandra.in.sh)
**Category**: Logic error in condition

---

## Root Cause

The RPM startup script `cassandra.in.sh` used string comparison (`"$JVM_VERSION" \< "11"`) to detect the Java version, which worked for Java 8 and 11 but silently produced the wrong result for Java 17: the raw `jvmver` string for Java 17 begins with "17", so the two-character prefix comparison `short=$(cut -c1-2)` was not performed, and the script fell through to the `elif [ "$JVM_VERSION" \< "17" ]` branch, treating JDK 17 as an unsupported version and aborting. Even when the abort was bypassed, `JAVA_VERSION` was never set to `17`, so the subsequent conditional that selects `jvm17-server.options` was never entered and the process started with the wrong options file.

### Before / After

```sh
# BEFORE – only handles Java 8 and 11; treats 17 as < "11" in string compare and exits
JAVA_VERSION=11
if [ "$JVM_VERSION" = "1.8.0" ] ; then
    ...
    JAVA_VERSION=8
elif [ "$JVM_VERSION" \< "11" ] ; then
    echo "Cassandra 4.0 requires either Java 8 ... "
    exit 1;
fi
...
if [ $JAVA_VERSION -ge 11 ] ; then
    JVM_DEP_OPTS_FILE=$CASSANDRA_CONF/jvm11${jvmoptions_variant:--clients}.options
```

```sh
# AFTER – sets default to 17, detects 11 by 2-char prefix, loads jvm17 options correctly
JAVA_VERSION=17
short=$(echo "${jvmver}" | cut -c1-2)
if [ "$short" = "11" ] ; then
    JAVA_VERSION=11
elif [ "$JVM_VERSION" \< "17" ] ; then
    echo "Cassandra 5.0 requires Java 11 or Java 17."
    exit 1;
fi
...
if [ $JAVA_VERSION -ge 17 ] ; then
    JVM_DEP_OPTS_FILE=$CASSANDRA_CONF/jvm17${jvmoptions_variant:--clients}.options
elif [ $JAVA_VERSION -ge 11 ] ; then
    JVM_DEP_OPTS_FILE=$CASSANDRA_CONF/jvm11${jvmoptions_variant:--clients}.options
fi
```

---

## How to Spot

Shell version-string comparisons with `\<` / `\>` are lexicographic, not numeric: "17" sorts before "9" in lexicographic order, and Java version strings changed format across major releases. Whenever a startup script adds support for a new major Java version, verify that the detection logic uses numeric or prefix-based comparisons and that the option-file selection chain includes a branch for every supported version.

---

## Test

No test added.
