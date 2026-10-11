# Bug 1279: cqlsh COPY TO Uses Wrong Write Meter Class

**Commit**: 93b36d4b82 · **Ticket**: CASSANDRA-9217
**Subsystem**: bin/cqlsh
**Category**: Wrong constant / default value

---

## Root Cause

The `COPY TO` export path in cqlsh instantiated `meter.Meter()` (a class from an imported module) to track write throughput, but the `COPY FROM` import path used an inline hand-rolled rate-tracking loop. The `meter.Meter` class had a different interface (`mark_written()`, `done()`, `num_finished()`) that did not accurately reflect the actual row count or rate for exports, and the final row count returned by `num_finished()` could diverge from reality. Additionally, the summary print statement lacked a leading newline, causing the final count message to overwrite the last progress line. The fix extracts a shared `RateMeter` class used by both paths, ensuring consistent counting and display.

### Before / After

```python
# BEFORE (COPY TO path)
wmeter = meter.Meter()
...
    writer.writerow(...)
    wmeter.mark_written()
wmeter.done()
...
return wmeter.num_finished()
```

```python
# AFTER (COPY TO path)
meter = RateMeter(10000)
...
    writer.writerow(...)
    meter.increment()
...
return meter.current_record
```

---

## How to Spot

When two code paths perform the same logical operation (tracking write progress) but use different implementations, the divergence is a maintenance hazard and a likely source of subtle behavioral differences. In CLI tools, always look for duplicate progress-tracking patterns and ask whether the reported count actually matches the loop variable. Additionally, `\r`-terminated progress lines must be followed by `\n` in the final summary to avoid the summary message overwriting itself on the terminal.

---

## Test

`pylib/cqlshlib/test/test_cqlsh_completion.py` updated (minor test fixture adjustments unrelated to the meter fix); no test specifically covers the rate meter behavior.
