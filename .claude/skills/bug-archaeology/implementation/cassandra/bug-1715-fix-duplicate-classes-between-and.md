# Bug 1715: Fix Duplicate Classes Between cassandra-all and cassandra-clientutil

**Commit**: c49d33633a · **Ticket**: CASSANDRA-5833
**Subsystem**: build (build.xml)
**Category**: Wrong constant / default value

---

## Root Cause

The Ant build script included `ITransportFactory.class` and `TFramedTransportFactory.class` verbatim into both the `cassandra-thrift` jar and the main `cassandra-all` jar, so both artifacts contained identical copies of those classes. The pattern `*.class` was needed to also capture inner classes (e.g., `ITransportFactory$Something.class`), and the main jar needed explicit excludes so the duplicates were removed.

### Before / After

```xml
<!-- BEFORE: exact names miss inner classes; main jar includes everything -->
<include name="org/apache/cassandra/thrift/ITransportFactory.class" />
<include name="org/apache/cassandra/thrift/TFramedTransportFactory.class" />
...
<jar jarfile="${build.dir}/${final.name}.jar"
     basedir="${build.classes.main}">
```

```xml
<!-- AFTER: wildcard captures inner classes; main jar excludes them -->
<include name="org/apache/cassandra/thrift/ITransportFactory*.class" />
<include name="org/apache/cassandra/thrift/TFramedTransportFactory*.class" />
...
<jar jarfile="${build.dir}/${final.name}.jar">
  <fileset dir="${build.classes.main}">
    <exclude name="org/apache/cassandra/thrift/ITransportFactory*.class" />
    <exclude name="org/apache/cassandra/thrift/TFramedTransportFactory*.class" />
  </fileset>
```

---

## How to Spot

When a build produces multiple jars from the same class directory without explicit include/exclude coordination, duplicate classes appear across artifacts. Review build scripts for any `<include>` list in one jar that is not mirrored by an `<exclude>` in sibling jars built from the same source directory.

---

## Test

No test added.
