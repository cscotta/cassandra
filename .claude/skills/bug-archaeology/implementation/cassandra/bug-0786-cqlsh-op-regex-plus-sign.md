# Bug 0786: CQLSH parser rejects `+` as operator due to misplaced character in regex

**Commit**: 8884302fac · **Ticket**: CASSANDRA-11935 (follow-up)
**Subsystem**: CQLSH / CQL parser
**Category**: Logic error in condition

---

## Root Cause
The ANTLR-style grammar rule for the `<op>` token in `cql3handling.py` used the regex `/[+-=%/,().]/ `. In a character-class bracket expression, when `+` is not the first or last character, placing `-` between `+` and `=` makes the parser interpret it as a range `+` through `=` (ASCII 43–61). The literal `+` character therefore fell inside an unintended range rather than being matched as itself. The fix reorders the characters so `-` appears first in the class (`[-+=%/,().]`), where it is unambiguously a literal hyphen and `+` is matched explicitly.

### Before / After
```python
# BEFORE
<op> ::=  /[+-=%/,().]/ ;
#          ^--- '-' between '+' and '=' acts as a range, not a literal

# AFTER
<op> ::=  /[-+=%/,().]/ ;
#          ^--- '-' first in class = literal hyphen; '+' now matched explicitly
```

---

## How to Spot
In any regex character class, a `-` that is not the first or last character (or immediately after a `[` or before a `]`) creates a range. When reviewing grammar or parser regex literals, verify that every `-` inside `[…]` is intentionally a range operator. If a character like `+` visually "surrounds" the `-`, the resulting range may silently include or exclude characters the author intended to handle explicitly.

---

## Test
No test added.
