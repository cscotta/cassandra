# Bug 0536: REST catalog swallows NotFoundException for missing metadata file as NoSuchTableException

**Commit**: 88d833b06bdb07101e458b8fdb7e60fe9e1a9c9a · **Ticket**: #13143

## Detection Rule

When an error-handler switch/if-chain converts HTTP 404 responses into typed exceptions, verify that every exception subtype the server can serialize is covered; an unrecognized `error.type()` string should not silently fall through to the wrong exception type.

## Code Smell

A `case 404` block that only checks for one or two specific exception class names and falls through to `throw new NoSuchTableException(...)` for all others, meaning any server-side `NotFoundException` (or future new type) is reported as a missing table.

## Tags

- scope: project-specific
- category: wrong-filter-result
- code-pattern: sentinel-not-handled
- severity: silent-wrong-result
- subsystem: core/rest-catalog

## Root Cause

`ErrorHandlers.java` mapped every 404 response that was not `NoSuchNamespaceException` to `NoSuchTableException`. When the REST catalog server throws a `NotFoundException` (e.g., because the metadata JSON file referenced in the catalog entry is absent from storage), the client received a `NoSuchTableException` instead, hiding the true nature of the error — a corrupt or partially-deleted table entry rather than a genuinely absent table.

## Example

```java
// BEFORE
case 404:
  if (NoSuchNamespaceException.class.getSimpleName().equals(error.type())) {
    throw new NoSuchNamespaceException("%s", error.message());
  } else {
    throw new NoSuchTableException("%s", error.message()); // catches NotFoundException too
  }
```

```java
// AFTER
case 404:
  if (NoSuchNamespaceException.class.getSimpleName().equals(error.type())) {
    throw new NoSuchNamespaceException("%s", error.message());
  } else if (NotFoundException.class.getSimpleName().equals(error.type())) {
    throw new NotFoundException("%s", error.message());
  } else {
    throw new NoSuchTableException("%s", error.message());
  }
```
