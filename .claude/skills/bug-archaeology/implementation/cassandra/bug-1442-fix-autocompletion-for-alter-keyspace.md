# Bug 1442: Fix Autocompletion for ALTER KEYSPACE

**Commit**: 1d02d7b34e · **Ticket**: CASSANDRA-8021
**Subsystem**: pylib/cqlshlib/cql3handling.py
**Category**: Silent skip on non-match

---

## Root Cause

The ANTLR-style grammar rule for `<alterKeyspaceStatement>` used an unnamed inline alternation `( "KEYSPACE" | "SCHEMA" )` for the keyword after `ALTER`. Without a named capture group, the cqlsh completion engine could not identify which token was consumed and therefore could not advance the completion cursor past the keyword to offer the keyspace name as the next completion suggestion. The fix assigns the capture label `wat=` to the alternation, making it a named token the completion logic can track.

### Before / After

```python
# BEFORE
<alterKeyspaceStatement> ::= "ALTER" ( "KEYSPACE" | "SCHEMA" ) ks=<alterableKeyspaceName>
```

```python
# AFTER
<alterKeyspaceStatement> ::= "ALTER" wat=( "KEYSPACE" | "SCHEMA" ) ks=<alterableKeyspaceName>
```

---

## How to Spot

In cqlsh grammar rules, every token or alternation that represents a keyword the user must type should carry a named label so the completion framework can advance its state machine past that keyword. An unnamed inline alternation between quoted keywords is a clear indicator that completion will stall at that position.

---

## Test

No test added.
