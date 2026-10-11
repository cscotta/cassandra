# Bug 2559: SSL keystore and truststore streams not closed, leaking file descriptors

**Commit**: 2cd49b5e6a · **Ticket**: CASSANDRA-3257

## Detection Rule
When `new FileInputStream(path)` is passed directly to `KeyStore.load()` inside a `try` block that lacks a `finally` close, each call to `createSSLContext` permanently leaks one or two file descriptors.

## Code Smell
A `FileInputStream` is constructed inline as an argument to a method call (`ts.load(new FileInputStream(...), ...)`) without being assigned to a variable, making it impossible to close in a `finally` block.

## Tags
- scope: cassandra
- category: resource-leak
- code-pattern: inline-stream-never-closed
- severity: crash
- subsystem: network-messaging

## Root Cause
`SSLFactory.createSSLContext()` opened `FileInputStream` objects for the truststore and keystore inline in the `ts.load()` and `ks.load()` calls. Neither stream was assigned to a variable, so there was no handle to close them. With internode encryption enabled, each new SSL context creation (which can happen frequently) leaked two file descriptors until the process hit the OS limit.

## Example
```java
// BEFORE
ts.load(new FileInputStream(options.truststore),
        options.truststore_password.toCharArray());
ks.load(new FileInputStream(options.keystore),
        options.keystore_password.toCharArray());
```
```java
// AFTER
FileInputStream tsf = new FileInputStream(options.truststore);
FileInputStream ksf = new FileInputStream(options.keystore);
try {
    ts.load(tsf, options.truststore_password.toCharArray());
    ks.load(ksf, options.keystore_password.toCharArray());
    ...
} finally {
    tsf.close();
    ksf.close();
}
```
