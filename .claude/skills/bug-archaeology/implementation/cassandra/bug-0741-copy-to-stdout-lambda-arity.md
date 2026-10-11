# Bug 0741: COPY TO STDOUT fails due to broken lambda signature in printmsg suppressor

**Commit**: 88f36a0955 · **Ticket**: CASSANDRA-12497
**Subsystem**: cqlsh / COPY
**Category**: Logic error in condition

---

## Root Cause
When exporting to STDOUT (no filename), `CopyTask` suppressed progress messages by replacing `self.printmsg` with a lambda `lambda _, eol='\n': None`. However, `printmsg` accepts three positional/keyword arguments (`msg`, `eol`, `encoding`); the lambda only accepted two. When any code path called `self.printmsg(msg, encoding='utf8')` or similar, Python raised a `TypeError` because the `encoding` keyword argument was unexpected, aborting the COPY export. The fix introduces a proper `swallowmsg(msg, eol='', encoding='')` function whose signature exactly mirrors `printmsg`, so keyword calls succeed silently.

### Before / After
```python
# BEFORE — wrong arity, crashes on printmsg(msg, encoding='utf8')
self.printmsg = printmsg if self.fname is not None or direction == 'from' or DEBUG \
    else lambda _, eol='\n': None

# AFTER — matching signature
self.printmsg = printmsg if self.fname is not None or direction == 'from' or DEBUG \
    else swallowmsg

def swallowmsg(msg, eol='', encoding=''):
    None
```

---

## How to Spot
Stub/no-op replacements for callable attributes must exactly match the signature (including all keyword arguments) of the original callable. In review, compare the no-op lambda or function against every call site of the attribute to ensure no `TypeError` can arise from extra keyword arguments.

---

## Test
No new test added.
