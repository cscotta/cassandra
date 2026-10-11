# Bug 1014: NPE when aborting SSTable writer on stream read error

**Commit**: a9a2d75b26 · **Ticket**: CASSANDRA-10771
**Subsystem**: Streaming
**Category**: Missing null / bounds check

---

## Root Cause
`StreamReader.read()` (and the compressed variant `CompressedStreamReader.read()`) catch any `Throwable` during SSTable writing and attempt to abort the in-progress writer.  The old code called `writer.abort(null)` — passing `null` as the accumulator argument — and then added the return value via `e.addSuppressed(e2)`.  Several `abort()` implementations propagate the accumulator directly and can return `null` when passed `null`, and `Throwable.addSuppressed(null)` throws a `NullPointerException`, masking the original read error.  The fix passes the original exception `e` as the accumulator to `writer.abort(e)`, which threads all abort errors onto the original exception without any null involvement.

### Before / After
```java
// BEFORE – abort(null) can return null; addSuppressed(null) throws NPE
if (writer != null)
{
    Throwable e2 = writer.abort(null);
    // add abort error to original and continue so we can drain unread stream
    e.addSuppressed(e2);   // NPE if e2 == null
}

// AFTER – pass original exception; abort chains errors onto it safely
if (writer != null)
{
    writer.abort(e);   // e is non-null; abort appends any additional errors
}
```

---

## How to Spot
The `Throwable.addSuppressed(Throwable)` contract requires a non-null argument — passing null throws `NullPointerException`.  Any call of the form `e.addSuppressed(someMethod(...))` where `someMethod` can return null will hide the original exception with a confusing NPE.  Prefer the accumulator-chaining pattern where the original exception is threaded through cleanup methods so all errors are attached before propagation.

---

## Test
No new test added; the fix is in the error-handling path of `StreamReader` and `CompressedStreamReader`.
