**Commit**: 37392d9bb7 · **Ticket**: KAFKA-15469
**Subsystem**: docs/configuration.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
</script>

// After
  <h3 class="anchor-heading">
    <a id="config_providers" class="anchor-link"></a>
    <a href="#config_providers">3.11 Configuration Providers</a>
  </h3>

  <p>
    Use configuration providers to load configuration data from external sources. This might include sensitive information, such as passwords, API keys, or other credentials.
  </p>

  <p>You have the following options:</p>
  <ul>
    <li>
```

**How to spot**
When reviewing docs/configuration.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-15469: add documentation for configuration providers (#16650).

**Test**
No test added.
