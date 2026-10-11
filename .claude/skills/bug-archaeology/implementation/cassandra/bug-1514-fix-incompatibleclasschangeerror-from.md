# Bug 1514: Fix IncompatibleClassChangeError from hadoop2

**Commit**: f83909e041 · **Ticket**: CASSANDRA-7229
**Subsystem**: src/java/org/apache/cassandra/hadoop/cql3
**Category**: Missing override of default method

---

## Root Cause

`CqlInputFormat.getRecordReader()` created an anonymous subclass of `TaskAttemptContext` overriding `progress()`. In Hadoop 2, `TaskAttemptContext` changed from a concrete class to an interface, making the anonymous subclass illegal and causing an `IncompatibleClassChangeError` at runtime. The fix replaces the anonymous subclass with a call to `HadoopCompat.newMapContext()`, which abstracts over Hadoop 1/2 differences and uses the existing `ReporterWrapper` adapter.

### Before / After

```java
// BEFORE
TaskAttemptContext tac = new TaskAttemptContext(jobConf, TaskAttemptID.forName(jobConf.get(MAPRED_TASK_ID)))
{
    @Override
    public void progress()
    {
        reporter.progress();
    }
};
```

```java
// AFTER
TaskAttemptContext tac = HadoopCompat.newMapContext(
        jobConf,
        TaskAttemptID.forName(jobConf.get(MAPRED_TASK_ID)),
        null,
        null,
        null,
        new ReporterWrapper(reporter),
        null);
```

---

## How to Spot

When code creates an anonymous subclass of an external library type, check whether that type is a class or interface across the versions supported. Anonymous subclasses of types that become interfaces in newer library versions will fail with `IncompatibleClassChangeError` at runtime rather than compile time.

---

## Test

No test added.
