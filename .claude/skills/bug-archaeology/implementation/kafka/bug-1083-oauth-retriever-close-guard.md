## Commit

c60c83aaba — KAFKA-18847 1/N: Rename AccessTokenRetriever/Validator to JwtRetriever/Validator

## Ticket

KAFKA-18847

## Subsystem

Security / OAuth SASL authentication

## Category

Resource leak

## Root cause

`OAuthBearerLoginCallbackHandler` and `OAuthBearerValidatorCallbackHandler` used an `isInitialized` flag to guard `close()` and relied on custom null checks for the retriever/validator instances. If `configure()` threw partway through, the partially-initialised retriever could be left open because `close()` checked `isInitialized` before calling the retriever's `close()`. Removing the flag and replacing direct `close()` calls with `Utils.closeQuietly` ensures that any non-null retriever or validator is always closed, even when initialisation fails mid-way.

## Before / After

```java
// Before — isInitialized guard skips close if configure() failed
@Override
public void close() {
    if (isInitialized) {
        retriever.close();
    }
}

// After — closeQuietly always runs for non-null instances
@Override
public void close() {
    Utils.closeQuietly(retriever, "JwtRetriever");
    Utils.closeQuietly(validator, "JwtValidator");
}
```

## How to spot

Search for `if (isInitialized)` or `if (fieldName != null)` guards inside `close()` methods. Any such guard that prevents `close()` from running when initialisation was partial is a resource-leak risk. Use `Utils.closeQuietly` to handle null safely without a separate flag.

## Test

`OAuthBearerLoginCallbackHandlerTest` was extended to verify that the retriever is closed even when `configure()` throws after the retriever is created.
