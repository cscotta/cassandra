# Bug 0380: EncryptingFileIO.properties() throws UnsupportedOperationException instead of delegating

**Commit**: 473d46a5c76ce7fc776199034650749572b3b1d5 · **Ticket**: #15289

## Detection Rule

When a class wraps another object and implements a delegate interface, verify that every default-throws method on the interface is explicitly overridden to forward to the wrapped object.

## Code Smell

`EncryptingFileIO` overrides `newInputFile`, `newOutputFile`, `deleteFile`, and `close` from `FileIO` but omits `properties()`, which has a default implementation that throws `UnsupportedOperationException`. Any caller that invokes `.properties()` on an `EncryptingFileIO` instance will crash at runtime.

## Tags

- scope: java
- category: missing-override
- code-pattern: delegate-wrapper-incomplete
- severity: crash
- subsystem: encryption/io

## Root Cause

`FileIO.properties()` has a default implementation that throws `UnsupportedOperationException`. `EncryptingFileIO` wraps a delegate `FileIO` but did not override `properties()` to forward to it. Because the omission is silent at compile time (no interface-enforcement mechanism), the crash only surfaces when code paths that call `.properties()` on the wrapper are exercised, such as catalog initialization or IO configuration propagation.

## Example

```java
// BEFORE: no properties() override — falls through to FileIO default that throws
public class EncryptingFileIO implements FileIO, Serializable {
  private final FileIO io;
  private final EncryptionManager em;

  @Override
  public void deleteFile(String path) {
    io.deleteFile(path);
  }
  // properties() not overridden → throws UnsupportedOperationException
}
```

```java
// AFTER: explicit delegation to wrapped FileIO
@Override
public Map<String, String> properties() {
  return io.properties();
}
```
