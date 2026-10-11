# Bug 0993: Stress Graph Regex Run on Every Line

**Commit**: 4675b2089f · **Ticket**: n/a
**Subsystem**: cassandra-stress / tooling
**Category**: Logic error in condition

---

## Root Cause
`StressGraph` parsed stress output line-by-line to extract the thread count when running multiple thread-count sweeps. The `threadCountMessage` regex `Matcher` was constructed and executed against every output line, regardless of whether multiple thread counts were actually being tested. The `runningMultipleThreadCounts` flag was checked only after the match, so the match result was discarded whenever single-thread-count mode was active. This was wasteful but also logically wrong: the outer guard should control whether the inner parsing is attempted at all.

### Before / After
```java
// BEFORE — matcher runs on every line unconditionally
Matcher tc = threadCountMessage.matcher(line);
if (tc.matches())
{
    if (runningMultipleThreadCounts)
    {
        currentThreadCount = tc.group(2);
    }
}

// AFTER — matcher only runs when actually needed
if (runningMultipleThreadCounts)
{
    Matcher tc = threadCountMessage.matcher(line);
    if (tc.matches())
    {
        currentThreadCount = tc.group(2);
    }
}
```

---

## How to Spot
When a flag guards the use of an expensive or logically conditional operation, verify the flag is checked *before* the operation rather than inside its result handler. Regex matching in a tight loop is a common place to find this pattern: the match is performed on every iteration but the result is conditionally ignored, meaning both the performance cost and any side effects of the match are paid unnecessarily.

---

## Test
No test added.
