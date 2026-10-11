**Commit**: b3fd9a5a95 · **Ticket**: KAFKA-17146
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
When reverting the ZK migration, we must also remove the /migration ZNode in order to allow the migration to be re-attempted in the future. Reviewers: Colin P. McCabe <cmccabe@apache.org>, Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
              of the brokers can become the new old-style controller.

// After
              of the brokers can become the new old-style controller. Additionally, run
              <code>get /migration</code> followed by <code>rmr /migration</code> to clear the
              migration state from ZooKeeper. This will allow you to re-attempt the migration
              in the future. The data read from "/migration" can be useful for debugging.
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17146 include note to remove migration znode (#16770).

**Test**
No test added.
