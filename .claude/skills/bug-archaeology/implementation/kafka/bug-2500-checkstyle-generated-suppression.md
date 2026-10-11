**Commit**: 76fb18f967 · **Ticket**: KAFKA-17416
**Subsystem**: build/checkstyle/suppressions.xml
**Category**: State not cleaned up

**Root cause**
`checkstyle/suppressions.xml` had ten separate suppression rules that each matched a specific module's generated code directory (e.g., `clients/build/generated/`, `streams/build/generated/`, `group-coordinator/build/generated/`). Each time a new module was added, a developer had to manually add another suppression rule. If the rule was forgotten, checkstyle would report hundreds of spurious violations in generated code. The fix replaces all ten module-specific rules with a single universal suppression that matches any path containing `/generated/`.

**Before/After**
```xml
<!-- Before: one rule per module -->
<suppress checks="..." files="clients[\\/]build[\\/]generated[\\/].+.java$"/>
<suppress checks="..." files="streams[\\/]build[\\/]generated[\\/].+.java$"/>
<!-- ... 8 more similar rules -->

<!-- After: one rule for all generated code -->
<suppress checks="[a-zA-Z0-9]*" files="[\\/]generated[\\/]"/>
```

**How to spot**
When a suppression file has multiple near-identical rules that differ only in a module path prefix, consolidate them into a single pattern that matches the shared path segment. Fragmented suppression rules require manual maintenance whenever a new module is added and lead to CI failures when someone forgets to add the new rule.

**Test**
No functional test; verified by running checkstyle against all modules and confirming no spurious failures on generated files.
