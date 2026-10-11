# Bug 3007: NPE when streaming because encryption_options is null

**Commit**: dfcb62163b · **Ticket**: CASSANDRA-3007

## Detection Rule
When reading an optional configuration object that may be null (e.g. `getEncryptionOptions()`), always null-check the result before accessing its fields.

## Code Smell
`if (DatabaseDescriptor.getEncryptionOptions().internode_encryption == ...)` will throw a NullPointerException when `encryption_options` is not set in cassandra.yaml and the method returns null.

## Tags
- scope: cassandra
- category: missing-null-check
- code-pattern: optional-config-null-dereference
- severity: crash
- subsystem: network-messaging

## Root Cause
`MessagingService.stream()` directly accessed the `internode_encryption` field of the object returned by `DatabaseDescriptor.getEncryptionOptions()` without checking for null. When `encryption_options` was omitted from `cassandra.yaml` the method returned null, causing an NPE during any streaming operation. The fix assigns the result to a local variable and adds a null guard.

## Example
```java
// BEFORE
if (DatabaseDescriptor.getEncryptionOptions().internode_encryption ==
        EncryptionOptions.InternodeEncryption.all)
    streamExecutor_.execute(new SSLFileStreamTask(header, to));
```
```java
// AFTER
EncryptionOptions encryption = DatabaseDescriptor.getEncryptionOptions();
if (encryption != null && encryption.internode_encryption ==
        EncryptionOptions.InternodeEncryption.all)
    streamExecutor_.execute(new SSLFileStreamTask(header, to));
```
