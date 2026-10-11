# Bug 1359: NPE When Passing Unquoted Keyword as ALTER TABLE Option Value

**Commit**: 25d4a13d56 · **Ticket**: CASSANDRA-8355
**Subsystem**: src/java/org/apache/cassandra/cql3/Cql.g
**Category**: Missing null / bounds check

---

## Root Cause

The CQL grammar rule `functionArgs` was reused in the `term` production to parse both function-call arguments and standalone values. When a user wrote an ALTER TABLE statement with an unquoted keyword as an option value (e.g., `'rows_per_partition' : ALL`), the parser matched `ALL` as a zero-argument function call and attempted to build a `FunctionCall.Raw` node, but the resulting term reference was `null` because the grammar did not wire up a return value for the zero-argument path through the old inline rule. This `null` propagated silently until dereference caused an NPE. The fix introduces a dedicated `function` grammar rule that cleanly handles both zero-argument and multi-argument function calls, ensuring a non-null `Term.Raw` is always returned.

### Before / After

```antlr
// BEFORE — functionArgs handled empty args inline; zero-arg path produced null term
functionArgs returns [List<Term.Raw> a]
    : '(' ')' { $a = Collections.emptyList(); }
    | '(' t1=term { List<Term.Raw> args = new ArrayList<Term.Raw>(); args.add(t1); }
          ( ',' tn=term { args.add(tn); } )*
       ')' { $a = args; }
    ;

term returns [Term.Raw term]
    : v=value                          { $term = v; }
    | f=functionName args=functionArgs { $term = new FunctionCall.Raw(f, args); }
    ...
```

```antlr
// AFTER — dedicated function rule guarantees non-null Term.Raw for both arities
function returns [Term.Raw t]
    : f=functionName '(' ')'                   { $t = new FunctionCall.Raw(f, Collections.<Term.Raw>emptyList()); }
    | f=functionName '(' args=functionArgs ')' { $t = new FunctionCall.Raw(f, args); }
    ;

functionArgs returns [List<Term.Raw> args]
    @init{ $args = new ArrayList<Term.Raw>(); }
    : t1=term {args.add(t1); } ( ',' tn=term { args.add(tn); } )*
    ;

term returns [Term.Raw term]
    : v=value     { $term = v; }
    | f=function  { $term = f; }
    ...
```

---

## How to Spot

In ANTLR grammars, return values from rules are `null` by default if not explicitly assigned in every branch. Reviewers should verify that every alternative in a grammar rule that contributes a return value (`returns [...]`) actually assigns that value in all of its production branches. Inline list-building patterns (constructing and assigning `args` inside the rule body rather than returning from a dedicated rule) are especially fragile because the assignment may be unreachable when an alternative matches a different path.

---

## Test

A regression test `testChangeStrategyWithUnquotedAgrument` was added to `AlterTableTest`: it attempts `ALTER TABLE ... WITH caching = {'keys': 'all', 'rows_per_partition': ALL}` (unquoted `ALL`) and asserts an `InvalidSyntaxMessage` rather than an NPE.
