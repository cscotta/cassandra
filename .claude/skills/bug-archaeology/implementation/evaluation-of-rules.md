# Evaluation: Semgrep Rules as Detection Heuristics

## Evaluation Criteria
- **Goal**: Flag suspect code for LLM deep-review. High recall > high precision.
- **Consumer**: An LLM agent that can cheaply triage false positives.
- **Failure mode to avoid**: False negatives (missing real bugs).

---

## Rule 1: Constant Mismatch (bug-0033)

**Would it fire on the bug?** Uncertain. The rule requires a very specific shape:
`if ($X > $CONST_A) { throw $EXC.apply(String.format("...", $CONST_B, ...)) }`.
The actual code uses `format()` (static import), throws via `exceptionBuilder.apply(format(...))`,
and the if-block doesn't use braces around the throw. The rule is OVER-SPECIFIED for detection.

**Noise level**: Low (good), but at the cost of low recall (bad for detection).

**Catches variants?** No. It misses:
- `throw new XException(format(...))` — no `.apply()`
- `throw new XException("..." + WRONG_CONST)` — no format call
- Logging with wrong constant (same class of bug, different action)
- `<=`, `>=`, `<`, `==` comparisons (not just `>`)

**Recommendation for detection**: MUCH LOOSER. The real pattern is:
"A conditional expression mentions identifier A, but a nearby string-formatting or
message-construction expression uses identifier B where A was likely intended."
A detection rule should fire on ANY if-block where the condition references one
constant and a throw/log/return within it references a different constant of the
same type.

---

## Rule 2: Missing Guard (bug-0028)

**Would it fire?** Only if the method literally says `return run(...)`. Actual code
may use other delegation patterns.

**Noise level**: Moderate — many methods delegate with `return run(...)` legitimately.

**Catches variants?** Poorly. The real pattern is broader: "public method on a
service interface doesn't handle the disabled/unavailable state." This manifests as:
- No null-check before using an injected dependency
- No feature-flag check before calling native code
- No state-enum check before dispatching

**Recommendation**: Too tight on the shape (`return run(...)`), too broad on the
trigger (ALL such methods). Better: flag public methods in classes that have an
`enabled`/`available`/`initialized` field but don't reference it.

---

## Rule 3: Lambda Capture (bug-0024)

**Would it fire?** Yes — `$COLLECTION.apply($PARAM -> { ... })` would match.

**Noise level**: Medium-high. Every `.apply(x -> {...})` fires. But this is
ACCEPTABLE for detection because the downstream agent can quickly assess whether
the code is on a hot path.

**Catches variants?** Partially. Misses `.forEach()`, `.map()`, `.accept()`, 
stream operations. The loop variant is good but should also include stream pipelines.

**Recommendation**: Good detection rule. Expand to cover `.forEach`, `.accept`,
`.map`, `.flatMap` with lambda bodies. The loop-context variant is a useful
severity escalator.

---

## Rule 4: Wrong Bound Type (bug-0026)

**Would it fire?** Yes — exact match on the API.

**Noise level**: Very low. Every match IS suspect.

**Catches variants?** NO — this is maximally specific to Cassandra's
`DataStorageSpec` API. It will never fire outside this codebase.

**Recommendation**: This is not a generalizable semgrep rule — it's a
project-specific lint. The GENERALIZED pattern is: "conversion method called on a
type whose unit semantics don't match the target unit." This is fundamentally hard
for semgrep because it requires understanding units. Better framed as a review
prompt: "flag any place where a value traverses a unit-conversion boundary and
verify the units match."

---

## Rule 5: Wrong Field in Containment (bug-0038)

**Would it fire?** The "parameter unused" variant would fire — that's a strong signal.

**Noise level**: Low for the unused-parameter variant. Methods named `contains*`
that don't use their parameter are almost always wrong.

**Catches variants?** The unused-parameter version is excellent. The compareTo
version is too specific (requires exact TxnId type annotation).

**Recommendation**: The unused-parameter heuristic is the best rule here. Generalize
further: any method whose name implies it checks a parameter (`contains`, `covers`,
`intersects`, `isValid`, `includes`, `matches`) but doesn't reference that parameter.
Drop the type restriction entirely.

---

## Overall Assessment

**Rules suited for detection (keep, loosen):** #1, #3, #5
**Rules too specific to this codebase:** #4 (project lint, not generalizable)
**Rules with wrong abstraction level:** #2 (shape too specific, concept too vague)

**Key principles for detection-oriented rules:**
1. Match STRUCTURE, not API names
2. Prefer absence-based rules (pattern-not) — "this expected thing is missing"
3. Accept noise — the LLM handles false positives cheaply
4. Each rule should be expressible as a natural-language review question
5. If you can't write the rule without project-specific types, it's a lint, not a detection heuristic
