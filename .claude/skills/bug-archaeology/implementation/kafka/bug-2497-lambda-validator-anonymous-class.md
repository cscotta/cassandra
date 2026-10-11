**Commit**: a39037e55c · **Ticket**: KAFKA-17399
**Subsystem**: clients/common/config/ConfigDef (and Streams/Connect)
**Category**: Wrong constant / default value

**Root cause**
Many sites in the codebase declared anonymous `ConfigDef.Validator` inner classes with both `ensureValid` and `toString` methods. Because `ConfigDef.LambdaValidator` already exists to express the same pattern with two lambdas, the verbose anonymous-class form added code noise without benefit. More critically, several sites implemented `toString()` incorrectly or omitted it, causing misleading validation-error messages. The fix replaces all such anonymous validators with `ConfigDef.LambdaValidator.with(ensureValidFn, toStringFn)` throughout `ConfigDef`, `StreamsConfig`, `ConnectorConfig`, `Cast`, and `ReplaceField`.

**Before/After**
```java
// Before: anonymous inner class
new ConfigDef.Validator() {
    @Override public void ensureValid(String name, Object value) { ... }
    @Override public String toString() { return "description"; }
}

// After: LambdaValidator
ConfigDef.LambdaValidator.with(
    (name, value) -> { /* validate */ },
    () -> "description")
```

**How to spot**
Search for `new ConfigDef.Validator()` anonymous class instantiations; each one should be replaced with `ConfigDef.LambdaValidator.with(...)`. If a site omits the `toString()` override the validator will produce a useless description in error messages.

**Test**
No new test; existing validator tests continue to pass with the refactored implementations.
