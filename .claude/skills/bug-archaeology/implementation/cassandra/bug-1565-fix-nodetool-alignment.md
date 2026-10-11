# Bug 1565: Fix nodetool Ring Output Column Alignment

**Commit**: d5265bb326 · **Ticket**: n/a
**Subsystem**: tools / NodeTool
**Category**: Wrong constant / default value

---

## Root Cause

The `nodetool ring` output used a `%-6s` format specifier for the "Tokens" column, which is only wide enough for 6 characters. With the default vnodes configuration a node can own many tokens, so the token count string is wider than 6 characters and the column overflows, misaligning every subsequent column. The fix widens the format string to `%-11s`.

### Before / After

```java
// BEFORE – too narrow for multi-digit token counts
buf.append("%-6s  ");   // "Tokens"
```

```java
// AFTER – wide enough to accommodate typical vnode counts
buf.append("%-11s  ");  // "Tokens"
```

---

## How to Spot

Format-string widths for columns that display numeric or variable-length data should be derived from the maximum expected value, not from the header label length. During review, verify that each `%-Ns` width is at least as wide as the longest value that could appear in that column.

---

## Test

No test added. The fix is a formatting constant change in the CLI output code.
