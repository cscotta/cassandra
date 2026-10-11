## Commit

- Hash: b35c29401a
- Tickets: KAFKA-18073
- Subject: KAFKA-18073: Throw exception instead of silently dropping record when retry budget exhausted in RetryWithToleranceOperator

## Subsystem

connect / runtime / error handling

## Category

Silent skip on non-match

## Root cause

When `RetryWithToleranceOperator` exhausted its retry budget on a retriable exception, it called `context.error(e)` to record the error but then returned `null` — effectively dropping the record. Callers that checked for `null` to detect a skipped record would see the drop, but callers that expected an exception to propagate for non-tolerated errors would silently lose data. The correct behaviour for "retries exhausted, error not tolerated" is to throw the exception so the connector task fails visibly.

## Before

```java
context.error(e);
return null;  // record silently dropped
```

## After

```java
context.error(e);
throw e;      // propagate so caller decides how to handle
```

## How to spot

Error-handling wrappers that record an error via a context object then return `null` without throwing — the null return silently swallows failures instead of surfacing them to the component that should decide on tolerance.

## Test

`RetryWithToleranceOperatorTest` extended: when tolerance is `none` and retries are exhausted, the operator now throws rather than returning null.
