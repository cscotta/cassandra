# Bug 0137: CQL Comment Stripping Corrupts String Literals Containing "/*"

**Commit**: a79f7f4b00 · **Ticket**: CASSANDRA-17667
**Subsystem**: bin/cqlsh.py · pylib/cqlshlib/cql3handling.py
**Category**: Silent skip on non-match

---

## Root Cause

`cqlsh.py` ran a pre-processing step (`strip_comment_blocks`) before passing statement text to the grammar-based lexer. This step used regular expressions to remove `/* ... */` comment blocks, but the regexes were not aware of quoted string literals: any single-quoted string that happened to contain `/*` or `*/` was partially erased. For example `'v1*/'` would be matched by `[/][*].*[*][/]` and deleted, leaving a malformed query. The fix removes the fragile pre-processing step entirely and instead moves the `<unclosedComment>` terminal production to appear *before* the `<unclosedString>` and `<unclosedName>` productions in the grammar, so the correct, context-aware lexer handles multi-line comment detection.

### Before / After

```python
# BEFORE – dangerous regex pre-pass in Shell.onecmd()
statementtext = self.strip_comment_blocks(statementtext)
statements, endtoken_escaped = cqlruleset.cql_split_statements(statementtext)
```

```python
# AFTER – no pre-pass; grammar handles comments natively
statements, endtoken_escaped = cqlruleset.cql_split_statements(statementtext)
```

```
# BEFORE – unclosed* terminals appear AFTER normal terminals in grammar
<unclosedPgString>::= ...
<unclosedString>  ::= ...
<unclosedName>    ::= ...
<unclosedComment> ::= /[/][*].*$/

# AFTER – unclosed* terminals moved to appear BEFORE <float> etc.
# (same patterns, but earlier in production order so they take priority)
```

---

## How to Spot

Whenever a parser pre-processes input with a regex before handing it to a proper lexer, check whether the regex respects all quoting and escaping contexts. A regex such as `[/][*].*[*][/]` with no awareness of surrounding quotes is a classic "silent mangle" that can delete valid user data without any error message.

---

## Test

A new test case in `test_cql_parsing.py` verifies that an INSERT statement whose string values contain `*/` and `/*` sub-strings is tokenized correctly with all `quotedStringLiteral` tokens intact.
