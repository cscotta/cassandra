# Bug 1485: Fix Parsing of UDF Body

**Commit**: f7cb008ceb · **Ticket**: CASSANDRA-7740
**Subsystem**: cql3 / grammar (Cql.g) + cqlsh
**Category**: Wrong serialization / deserialization

---

## Root Cause

The UDF (User-Defined Function) grammar used a `BODY … END BODY` delimiter syntax to wrap the function body. This syntax was replaced with the standard SQL `AS <string_literal>` convention, but the grammar was not updated: `Cql.g` still matched `K_LANGUAGE l=IDENT K_BODY body=(~K_END_BODY)* K_END_BODY` instead of `K_LANGUAGE l=IDENT K_AS body=STRING_LITERAL`. As a result, valid `CREATE FUNCTION … LANGUAGE java AS '...'` statements failed to parse. The fix also removes the now-dead `K_BODY` and `K_END_BODY` lexer tokens. The cqlsh Python grammar is updated in parallel to recognise the new `FUNCTION` keyword and the `AS <stringLiteral>` body form.

### Before / After

```antlr
// BEFORE (Cql.g)
| ( K_LANGUAGE l = IDENT { language=$l.text; } K_BODY
      body = ((~K_END_BODY)*)
    { bodyOrClassName = $body.text; } K_END_BODY )
```

```antlr
// AFTER (Cql.g)
| ( K_LANGUAGE l = IDENT { language=$l.text; } K_AS
    (
      ( body = STRING_LITERAL { bodyOrClassName = $body.text; } )
    )
  )
```

---

## How to Spot

Grammar changes that introduce a new syntax form must be applied consistently to both the server-side ANTLR grammar and any client-side parsers (cqlsh). When removing lexer tokens, confirm all rules that reference them are also updated, including `basic_unreserved_keyword` lists that allow tokens to double as identifiers.

---

## Test

No test added in this commit.
