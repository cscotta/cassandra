**Commit**: 8db86c60a6 · **Ticket**: KAFKA-17385
**Subsystem**: core/tools (AclCommand)
**Category**: State not cleaned up

**Root cause**
`AclCommand` maintained a dual code path: the `AdminClientService` (KRaft/bootstrap) path and the `AuthorizerService` (ZooKeeper direct) path, selectable via `--authorizer`, `--authorizer-properties`, and `--zk-tls-config-file` options. All three ZooKeeper-direct options were deprecated; with ZooKeeper removed in 4.0, the `AuthorizerService` class became dead code. Keeping it required maintaining the ZooKeeper client TLS wiring, the `AclAuthorizer` import, and complex mutual-exclusion argument validation that confused users about which options were still valid.

**Before/After**
```scala
// Before – conditional service selection
val aclCommandService =
  if (opts.options.has(opts.bootstrapServerOpt) || ...) new AdminClientService(opts)
  else new AuthorizerService(authorizerClassName, opts)

// After – unconditional
val aclCommandService = new AdminClientService(opts)
// --authorizer / --authorizer-properties / --zk-tls-config-file removed
```

**How to spot**
When removing a subsystem, find all CLI tools that have an `if (hasBootstrap) ... else useSubsystem()` pattern and collapse them to the bootstrap path only, removing deprecated option definitions and the dead `else` branch.

**Test**
`AclCommandTest` updated; ZK-specific test cases replaced with a single test for missing bootstrap options.
