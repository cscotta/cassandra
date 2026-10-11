# Bug 1024: CQLSH DESCRIBE Help Text Incomplete and Inaccurate

**Commit**: f319f0a236 · **Ticket**: CASSANDRA-10719
**Subsystem**: CQL shell (cqlsh)
**Category**: Wrong constant / default value

---

## Root Cause
The help text for the `DESCRIBE` command in `bin/cqlsh` contained several inaccuracies and omissions. The description of `DESCRIBE KEYSPACE` only mentioned tables but ignored types and other objects. `DESCRIBE TABLE` lacked the optional `[<keyspace>.]` qualifier syntax. And two new sub-commands — `DESCRIBE TYPES` and `DESCRIBE TYPE` — were implemented in the shell but never documented in the help text, leaving users unaware of their existence.

### Before / After
```python
# BEFORE — DESCRIBE TABLE had no keyspace qualifier in docs
DESCRIBE TABLE <tablename>

# AFTER — keyspace prefix documented
DESCRIBE TABLE [<keyspace>.]<tablename>

# BEFORE — no DESCRIBE TYPES / DESCRIBE TYPE entries

# AFTER — added:
DESCRIBE TYPES

  Output the names of all user-defined-types in the current keyspace, or in all
  keyspaces if there is no current keyspace.

DESCRIBE TYPE [<keyspace>.]<type>

  Output the CQL command that could be used to recreate the given user-defined-type.
```

---

## How to Spot
When a new CQLSH sub-command or option is added, check that the corresponding `do_describe` / `help_describe` method's docstring or help block is updated in lock-step. Reviewing the help output against the actual command dispatch table after any CLI feature addition would surface such omissions immediately. A simple grep for all `DESCRIBE` handler branches against every `DESCRIBE` keyword mentioned in the help block is a lightweight review check.

---

## Test
No test added.
