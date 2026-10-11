**Commit**: ff4d951027 · **Ticket**: KAFKA-17715

**Subsystem**: System Tests / kafka.py

**Category**: Stale/dead code removal

**Root cause**
`kafka_acls_cmd_with_optional_security_settings` accepted a `force_use_zk_connection` parameter that forced the ACL command to use ZooKeeper instead of `--bootstrap-server`. ZooKeeper ACL support has been removed, and all broker versions covered by the e2e test matrix (2.1.0 and above) support `--bootstrap-server` for ACL commands. The parameter was therefore always unused and could mask test failures if left in place. Removing it simplifies the helper and ensures all ACL command invocations use the bootstrap-server path.

**Before/After**
```python
# Before: force_use_zk_connection parameter accepted but always false
def kafka_acls_cmd_with_optional_security_settings(
        self, node, force_use_zk_connection, kafka_security_protocol=None):
    force_use_zk_connection = (force_use_zk_connection or
        not self.all_nodes_acl_command_supports_bootstrap_server)

# After: parameter removed; bootstrap-server always used
def kafka_acls_cmd_with_optional_security_settings(
        self, node, kafka_security_protocol=None):
    bootstrap_server = "--bootstrap-server %s" % self.bootstrap_servers(...)
```

**How to spot**
Helper functions with boolean "force legacy path" parameters are likely stale after the legacy path is removed. Search for `force_use_zk_connection` (or similar legacy-path flags) in test helpers and remove them once all supported versions use the modern path.

**Test**
All 90 affected e2e test scenarios pass without the `force_use_zk_connection` parameter, confirming that ACL commands work correctly via `--bootstrap-server` across all tested versions.
