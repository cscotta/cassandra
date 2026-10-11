# Bug Archaeology — Spec

## Purpose

Systematically extract knowledge from bug-fix commits in the Cassandra codebase.
Each entry captures the *why* — the mental model failure, the structural vulnerability —
so the patterns can be recognized before they bite again.

## How to generate an entry

1. `git show <commit>` — read the diff in full
2. Identify the **subsystem** and **category** (see taxonomy below)
3. Explain the **root cause** in 2–4 sentences with the key insight
4. Show a minimal **before/after** code snippet (≤ 30 lines total)
5. Write a **how to spot** paragraph: the heuristic that would catch this class of bug during review
6. Describe the **test** (what scenario + what assertion), or note "No test added"

## File naming

`bug-NNN-<slug>.md` where NNN is zero-padded to three digits and slug is 3–5 kebab-case words.

## Header format (required fields, in order)

```
**Commit**: <hash> · **Ticket**: <CASSANDRA-XXXXX or n/a>
**Subsystem**: <subsystem path or module name>
**Category**: <one of the taxonomy entries below>
```

No author lines.

## Bug category taxonomy

| Category | Description |
|----------|-------------|
| Missing override of default method | `default` returning a constant hides missing impl in subclass |
| Silent skip on non-match | regex/parser drops input without warning |
| Non-atomic file write | in-place overwrite without fsync+rename |
| Missing null / bounds check | NPE or IOOBE at an unchecked dereference |
| Off-by-one | fence-post error in range or index arithmetic |
| Race condition / missing synchronization | concurrent access without adequate locking |
| Deadlock | lock ordering violation or recursive lock |
| Resource leak | AutoCloseable not closed, or closed while still in use |
| Wrong constant / default value | hardcoded or default value wrong for the context |
| State not cleaned up | residual state from an earlier epoch/session causes incorrect behavior |
| Wrong serialization / deserialization | encoding mismatch, missing field, wrong type |
| Incorrect filtering / wrong query result | predicate, range check, or aggregation produces wrong answer |
| Logic error in condition | boolean expression, comparison operator, or branch inverted |

## Progress tracking

See `PROGRESS.md` for per-bug status.

## Resuming after interruption

1. Read `PROGRESS.md` — anything `pending` needs a file written
2. Use `git show <commit>` for each pending commit
3. Follow the spec above to write the file
4. Update `PROGRESS.md` status to `done`
