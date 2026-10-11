# Bug 0129: CQLSH CAPTURE Writes Trace Output to Capture File

**Commit**: 510c20fec1 · **Ticket**: CASSANDRA-19105
**Subsystem**: pylib/cqlshlib
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

When both `TRACING` and `CAPTURE` were enabled in cqlsh, the `print_trace` function wrote trace output to the shell's current `query_out` stream, which had already been redirected to the capture file by the CAPTURE command. As a result, trace details were appended to the capture file alongside query results, defeating the purpose of using CAPTURE to save clean query output. A secondary issue existed in the syntax grammar: `CAPTURE OFF` was parsed under the `fname` binding, which expected a string literal, so the `OFF` token was not recognized.

### Before / After

```python
# BEFORE (tracing.py) — trace written straight to whatever query_out is (the capture file)
def print_trace(shell, trace):
    rows = make_trace_rows(trace)
    ...
    shell.print_formatted_result(...)
```

```python
# AFTER — temporarily restore original query_out while printing trace
def print_trace(shell, trace):
    temp_query = None
    if shell.shunted_query_out is not None:
        temp_query = shell.query_out
        shell.query_out = shell.shunted_query_out
        ...
    rows = make_trace_rows(trace)
    ...
    if temp_query is not None:
        shell.query_out = temp_query
```

---

## How to Spot

Any code path that temporarily redirects an output stream (a "shunt") must be audited for every secondary output producer that executes during the redirect. When CAPTURE is active, `shunted_query_out` holds the original terminal stream; any code that ignores `shunted_query_out` and writes through `query_out` directly will contaminate the capture file. The grammar fix (`fname` vs `switch` binding) is a separate but related issue—`CAPTURE OFF` was not recognized as a keyword in the named binding that the handler read.

---

## Test

No dedicated test added; the fix is verified manually or through integration-style CQLSH tests.
