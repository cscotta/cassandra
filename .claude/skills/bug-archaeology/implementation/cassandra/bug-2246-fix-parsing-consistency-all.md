# Bug 2246: CQL3 Grammar Could Not Parse Consistency Level ALL

**Commit**: 8edf6a6192 · **Ticket**: CASSANDRA-4659
**Subsystem**: src/java/org/apache/cassandra/cql3/Cql.g
**Category**: Logic error in condition

---

## Root Cause

In the CQL3 ANTLR grammar, `K_ALL` was defined as a keyword token (`K_ALL: A L L`) and then referenced in the `K_LEVEL` rule as an alternative. ANTLR resolves keyword tokens greedily: because `K_ALL` was also used elsewhere as a keyword (e.g., in `SELECT *` contexts or future grammar rules), the lexer tokenized `ALL` as `K_ALL` and then the parser attempted to match the token `K_ALL` inside a rule that listed `K_ALL` as an alternative — but the rule itself expected the literal characters, causing a parse failure. The fix inlines the `A L L` character sequence directly in `K_LEVEL`, removing the separate `K_ALL` token definition, so `ALL` is unambiguously recognized as a consistency level.

### Before / After

```antlr
// BEFORE
K_ALL:         A L L;
K_LEVEL:       ( O N E | Q U O R U M | K_ALL | A N Y | ... );
```

```antlr
// AFTER
K_LEVEL:       ( O N E | Q U O R U M | A L L | A N Y | ... );
```

---

## How to Spot

In ANTLR grammars, referencing a named keyword token inside another lexer rule creates ambiguity. Consistency-level and similar enumeration rules should inline their character alternatives directly rather than delegating to separately defined keyword tokens.

---

## Test

No test added.
