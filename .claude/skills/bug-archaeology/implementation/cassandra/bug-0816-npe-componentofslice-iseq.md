# Bug 0816: NPE in ComponentOfSlice.isEQ()

**Commit**: 79a16e5e97 · **Ticket**: CASSANDRA-12706
**Subsystem**: Storage / Read Path
**Category**: Missing null / bounds check

---

## Root Cause
`ComponentOfSlice.isEQ()` in `Slices.java` called `startValue.equals(endValue)` directly on the extracted clustering component. When the slice bound has a null component (e.g., an inclusive end bound that contains no value for that column position), `startValue` or `endValue` can be `null`, causing an NPE. Replacing the call with `Objects.equals(startValue, endValue)` handles the null case safely.

### Before / After
```java
// BEFORE
public boolean isEQ()
{
    return startValue.equals(endValue);
}

// AFTER
public boolean isEQ()
{
    return Objects.equals(startValue, endValue);
}
```

---

## How to Spot
Any time an equality check is performed on a value extracted from a slice bound or clustering component, consider that the value may be absent (null) when the bound does not fully specify every clustering column. Calls to `.equals()` directly on such extracted values — without a prior null guard — should be flagged in review and replaced with `Objects.equals()`.

---

## Test
`SinglePartitionSliceCommandTest.toCQLStringIsSafeToCall()` constructs a `SinglePartitionReadCommand` with an inclusive-end slice and calls `toCQLString()`, which internally triggers `ComponentOfSlice.isEQ()`. The test asserts the result is non-null and non-empty, confirming no NPE is thrown.
