# Bug 2483: NPE when encryption options are absent from cassandra.yaml

**Commit**: 5ae93209f2 · **Ticket**: CASSANDRA-3489

## Detection Rule

When a configuration field is declared without a default value (null) and consumer code null-checks it at one call site but not another, any code path that skips the null check will throw a NullPointerException.

## Code Smell

`DatabaseDescriptor.getEncryptionOptions() != null &&` guard present in one place but the field is declared as `public EncryptionOptions encryption_options;` (null default) — omitting `= new EncryptionOptions()` means any other caller that skips the null check will NPE.

## Tags

- scope: cassandra
- category: missing-null-check
- code-pattern: null-config-field-no-default
- severity: crash
- subsystem: network-messaging

## Root Cause

`Config.encryption_options` was declared without an initializer, so it defaulted to `null` when the YAML file omitted the `encryption_options` stanza. `MessagingService.getServerSocket()` checked for null before checking `internode_encryption`, but other code paths did not. The fix initializes the field to `new EncryptionOptions()` and removes the redundant null check.

## Example

```java
// BEFORE
public EncryptionOptions encryption_options;  // null if absent from YAML
```

```java
// AFTER
public EncryptionOptions encryption_options = new EncryptionOptions();  // safe default
```
