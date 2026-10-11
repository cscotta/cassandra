# Bug 0465: cqlsh Output Corrupt When Fetching All Rows in Batch Mode

**Commit**: 9251b8116f · **Ticket**: CASSANDRA-15905
**Subsystem**: `bin/cqlsh.py`
**Category**: Incorrect filtering / wrong query result

---

## Root Cause

The cqlsh paging loop only entered its multi-page path when `result.has_more_pages and self.tty` was true. When running in non-TTY (batch/pipe) mode with a single-page result set, the code took a separate branch that always rendered the column header. For multi-page results in non-TTY mode, the header was printed once per page and an extra blank line separator was written between pages — both wrong for machine-readable output. The refactored `print_all` helper correctly gates header printing to the first page and blank-line emission to TTY mode only.

### Before / After

```python
# BEFORE
if result.has_more_pages and self.tty:
    num_rows = 0
    while True:
        if result.current_rows:
            num_rows += len(result.current_rows)
            self.print_static_result(result, table_meta)
        if result.has_more_pages:
            raw_input("---MORE---")
            result.fetch_next_page()
        else:
            break
else:
    num_rows = len(result.current_rows)
    self.print_static_result(result, table_meta)
```

```python
# AFTER
def print_all(result, table_meta, tty):
    num_rows = 0
    isFirst = True
    while True:
        if result.current_rows or isFirst:
            num_rows += len(result.current_rows)
            with_header = isFirst or tty
            self.print_static_result(result, table_meta, with_header, tty)
        if result.has_more_pages:
            if self.shunted_query_out is None and tty:
                raw_input("---MORE---")
            result.fetch_next_page()
        else:
            if not tty:
                self.writeresult("")
            break
        isFirst = False
    return num_rows
```

---

## How to Spot

Code that has separate fast-path and paging-path branches is prone to diverging behavior. Whenever a single helper is replaced by an if/else with a "simple" branch and a "paging" branch, check that both branches produce identical structural output (headers, separators, row count) regardless of the number of pages. Passing `tty` and `with_header` flags all the way through the call stack avoids silent mismatches between interactive and non-interactive modes.

---

## Test

No test added.
