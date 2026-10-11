**Commit**: e2d9ced098 · **Ticket**: KAFKA-18860
**Subsystem**: docs/ops.html
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
  <h4 class="anchor-heading"><a id="kraft_missing" class="anchor-link"></a><a href="#kraft_missing">Missing Features</a></h4>

  <p>The following features are not fully implemented in KRaft mode:</p>

  <ul>
    <li>Modifying certain dynamic configurations on the standalone KRaft controller</li>
  </ul>

                        <li><a href="#kraft_missing">Missing Features</a>

// After
// (no additions)
```

**How to spot**
When reviewing docs/ops.html code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18860 remove missing features section (#19048).

**Test**
No test added.
