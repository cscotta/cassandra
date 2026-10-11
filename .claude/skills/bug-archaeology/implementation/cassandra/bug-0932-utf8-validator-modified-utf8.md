# Bug 0932: UTF8Validator Accepts Invalid Modified UTF-8

**Commit**: 42cd66a1f9 · **Ticket**: CASSANDRA-10748
**Subsystem**: Type serialization / validation
**Category**: Logic error in condition

---

## Root Cause
In `UTF8Serializer.validate()`, the state machine that parses multi-byte UTF-8 sequences had a structural flaw: the `else if` branch that set `state = State.TWO` for valid 2-byte lead bytes was written as a plain `state = State.TWO` statement after the special-case `if (b == 0xc0)` block, rather than in an `else` branch of it. This meant that when `b == 0xc0` (the modified UTF-8 null prefix), the code first set `state = State.TWO_80` (correct) and then immediately overwrote it with `state = State.TWO` (wrong), causing the validator to accept a `0xc0` lead byte followed by any second byte instead of only `0x80`. As a result, modified UTF-8 sequences with `0xc0` followed by a non-`0x80` byte were incorrectly accepted as valid.

### Before / After
```java
// BEFORE — TWO_80 state immediately overwritten by TWO
if (b == (byte) 0xc0)
    state = State.TWO_80;
else if ((b & 0x1e) == 0)
    return false;
state = State.TWO;   // <- always executed, clobbers TWO_80

// AFTER — TWO is only set in the else branch
if (b == (byte) 0xc0)
    state = State.TWO_80;
else if ((b & 0x1e) == 0)
    return false;
else
    state = State.TWO;
```

---

## How to Spot
State-machine validators that use cascaded `if / else if` blocks without a final `else` before a fall-through assignment are prone to unintended state overwriting. During review, check that every path through a state-machine branch sets exactly one state and that no unconditional assignment follows a conditional one at the same indentation level.

---

## Test
No dedicated test added for this fix (the change is small and targeted); coverage relies on the existing UTF-8 validation test suite.
