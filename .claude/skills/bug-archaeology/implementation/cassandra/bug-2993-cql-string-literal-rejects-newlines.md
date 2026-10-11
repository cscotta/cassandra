# Bug 2993: CQL string literal lexer rejects newlines inside quoted strings

**Commit**: 4ffc89bb9b · **Ticket**: CASSANDRA-2993

## Detection Rule
When the CQL grammar's `STRING_LITERAL` rule uses `~('\''|'\r'|'\n')` to match content characters, it will reject multi-line string parameters in CQL queries, causing parse errors for values containing newlines.

## Code Smell
A `STRING_LITERAL` lexer rule that negates `'\r'` and `'\n'` in addition to the quote character will fail to tokenise any string containing embedded newlines, silently breaking parameterised queries with newline values.

## Tags
- scope: cassandra
- category: wrong-filter-result
- code-pattern: lexer-rule-rejects-newlines
- severity: crash
- subsystem: cql-query

## Root Cause
The CQL grammar lexed string literals with `c=~('\''|'\r'|'\n')`, treating carriage return and newline as invalid within a string literal. When Python (or other) drivers submitted query parameters that contained newline characters the lexer would reject them or produce incorrect tokens. The fix removes `'\r'` and `'\n'` from the exclusion set, allowing newlines inside string literals.

## Example
```antlr
// BEFORE
STRING_LITERAL
    : '\''
      ( c=~('\''|'\r'|'\n') { b.appendCodePoint(c);}
      | '\'' '\''            { b.appendCodePoint('\'');}
      )*
      '\''
```
```antlr
// AFTER
STRING_LITERAL
    : '\''
      ( c=~('\'') { b.appendCodePoint(c);}
      | '\'' '\''  { b.appendCodePoint('\'');}
      )*
      '\''
```
