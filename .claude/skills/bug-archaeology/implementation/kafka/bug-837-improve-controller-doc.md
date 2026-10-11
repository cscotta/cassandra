**Commit**: a058123cd8 · **Ticket**: KAFKA-19563
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
JIRA: [KAFKA-19563](https://issues.apache.org/jira/browse/KAFKA-19563)
Improve the add-controller doc to notify users they have to add the
configs to be passed to admin client into the controller.properties
before the improvement is done. ```
--command-config COMMAND_CONFIG
      Property file containing configs to be passed to Admin Client. For  add-controller,  the file is used to specify the controller
properties as well. ```

Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
  <p>Note that if there are any configs needed to be passed to the Admin Client, like the authentication configuration,
    please also include in the "controller.properties".</p>

```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19563: improve the add controller doc (#20261).

**Test**
No test added.
