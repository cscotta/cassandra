# Bug 0227: CQL Lexer Misparses Identifiers Starting with Capital P as ISO 8601 Duration

**Commit**: 0f3a990dd2 · **Ticket**: CASSANDRA-17919
**Subsystem**: cql3/antlr (Lexer.g)
**Category**: Silent skip on non-match

---

## Root Cause

The ANTLR `DURATION` lexer rule matched `'-'? 'P' ...` patterns that could greedily consume tokens beginning with the letter `P` before the `IDENT` rule had a chance to match. A table named `P`, a keyspace named `P`, or a `DURATION` column containing an ISO 8601 value like `PT0S` would cause parse errors because the original rule allowed zero-component ISO 8601 forms (e.g., `P` alone with empty optional groups). The fix refactored the ISO 8601 branch into helper fragment rules that require at least one explicit digit group, eliminating the zero-length ambiguity.

### Before / After

```antlr
// BEFORE
DURATION
    : '-'? 'P' (DIGIT+ 'Y')? (DIGIT+ 'M')? (DIGIT+ 'D')? ('T' ...)? // all optional, matches bare 'P'
    | '-'? 'P' DIGIT+ 'W'
    ;
```

```antlr
// AFTER
fragment DURATION_ISO_8601_PERIOD_DESIGNATORS
    : '-'? 'P' DIGIT+ 'Y' (DIGIT+ 'M')? (DIGIT+ 'D')?
    | '-'? 'P' DIGIT+ 'M' (DIGIT+ 'D')?
    | '-'? 'P' DIGIT+ 'D'
    ;

DURATION
    : '-'? DIGIT+ DURATION_UNIT (DIGIT+ DURATION_UNIT)*
    | '-'? 'P' DIGIT DIGIT DIGIT DIGIT '-' ...   // ISO 8601 alternative format
    | '-'? 'P' DURATION_ISO_8601_TIME_DESIGNATORS
    | DURATION_ISO_8601_WEEK_PERIOD_DESIGNATOR
    | DURATION_ISO_8601_PERIOD_DESIGNATORS DURATION_ISO_8601_TIME_DESIGNATORS?
    ;
```

---

## How to Spot

Lexer rules that use multiple fully-optional groups after a fixed prefix can silently match zero-length tokens, shadowing identifier or keyword rules. During grammar review, enumerate all paths through optional-group sequences and check whether the rule can fire when all optional parts are absent; if so, it will conflict with `IDENT`.

---

## Test

Added `testCreateTableWithNameCapitalPAndColumnDuration`, `testCreateKeyspaceWithNameCapitalP`, `testDropTableWithNameCapitalPAndColumnDuration`, and `testInsertZeroDuration` in `CreateTest`, `DropTest`, and `InsertTest`; verifies DDL and DML with identifiers/values that previously triggered parse errors.
