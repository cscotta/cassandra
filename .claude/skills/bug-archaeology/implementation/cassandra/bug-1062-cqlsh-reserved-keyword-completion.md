# Bug 1062: cqlsh Tab Completion Breaks on Reserved CQL Keywords as Identifiers

**Commit**: 806378c8c2 · **Ticket**: CASSANDRA-10415
**Subsystem**: cqlsh / Parsing Rule Engine
**Category**: Incorrect filtering / wrong query result

---

## Root Cause
The cqlsh completion engine's `CqlParsingRuleSet` had no concept of reserved CQL keywords. When a user typed an identifier that happened to be a reserved word (e.g. `TYPE`, `TOKEN`, `PRIMARY`), the parser treated it as a generic `<identifier>` token, which caused ambiguity in the completion grammar and produced incorrect or missing completions. The fix adds a `<reserved_identifier>` terminal to the grammar, populated at startup by all reserved keywords sourced from the Python Cassandra driver (`cassandra.metadata.cql_keywords_reserved`), and registers them via a new `set_reserved_keywords()` call in `CqlParsingRuleSet.__init__`. By making reserved words their own grammar token, the rule engine can correctly reject or route them separately from user-defined identifiers.

### Before / After
```python
# BEFORE — no reserved keyword rule registered; reserved words fell through as <identifier>
class CqlParsingRuleSet(pylexotron.ParsingRuleSet):
    def __init__(self):
        ...
        self.commands_end_with_newline = set()
        # (no keyword registration)

# AFTER
from cassandra.metadata import cql_keywords_reserved

class CqlParsingRuleSet(pylexotron.ParsingRuleSet):
    def __init__(self):
        ...
        self.commands_end_with_newline = set()
        self.set_reserved_keywords(cql_keywords_reserved)

    def set_reserved_keywords(self, keywords):
        syntax = '<reserved_identifier> ::= /(' + \
            '|'.join(r'\b{}\b'.format(k) for k in keywords) + ')/ ;'
        self.append_rules(syntax)
```

---

## How to Spot
If a completion grammar uses a single `<identifier>` token for all user-facing names without explicitly carving out reserved words, any schema or query involving a reserved word as an identifier will confuse the rule engine. During review, check whether the grammar distinguishes `<reserved_identifier>` from `<identifier>` and that the reserved set is kept in sync with the CQL specification (ideally by importing it from a canonical source such as the driver).

---

## Test
`test_cqlsh_completion.py` was updated to cover completion scenarios involving reserved keyword handling.
