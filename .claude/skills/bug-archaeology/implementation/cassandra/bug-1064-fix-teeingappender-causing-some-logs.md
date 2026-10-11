# Bug 1064: TeeingAppender Causes Truncated or Empty Log Output

**Commit**: 0556fbfa59 · **Ticket**: CASSANDRA-10447
**Subsystem**: test/unit/org/apache/cassandra/TeeingAppender.java
**Category**: Missing override of default method

---

## Root Cause

`TeeingAppender` extends `UnsynchronizedAppenderBase` but did not override `stop()`. When the logging framework shut down the appender it called only the base `stop()`, which stopped the container appender without first calling `detachAndStopAllAppenders()` on the child appenders it delegates to. Child appenders were therefore stopped in an uncontrolled order (or not at all), racing with in-flight log events and producing truncated or empty output.

### Before / After

```java
// BEFORE — no stop() override; child appenders not detached/stopped before super.stop()
public class TeeingAppender<E> extends UnsynchronizedAppenderBase<E> implements AppenderAttachable<E> {
    // ... no stop() method
}
```

```java
// AFTER — child appenders are stopped first
@Override
public void stop()
{
    try
    {
        if (started)
            detachAndStopAllAppenders();
    }
    finally
    {
        super.stop();
    }
}
```

---

## How to Spot

Whenever a composite appender class (one that wraps or delegates to other appenders) does not override `stop()`, check whether the parent's `stop()` actually tears down the children in the right order. The pattern to look for is: a class that holds references to child `Appender` objects, calls `addAppender()` / `detachAndStopAllAppenders()` elsewhere, but has no `stop()` override. On shutdown, child appenders may still have buffered data that will be lost once the parent marks itself stopped.

---

## Test

No test added (the change is in the appender implementation itself, which lives under the test tree; the fix is validated indirectly by the test infrastructure that relies on it).
