**Commit**: ef6c950b88 · **Ticket**: KAFKA-17767
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
                        default="build/junit-xml/**/*.xml",
                        help="Path to XML files. Glob patterns are supported.")
    reports = glob(pathname=args.path, recursive=True)
            logger.debug(f"Parsing {report}")

// After
from collections import OrderedDict
import pathlib
import re
import yaml

# Java method names can start with alpha, "_", or "$". Following characters can also include digits
method_matcher = re.compile(r"([a-zA-Z_$][a-zA-Z0-9_$]+).*")


def clean_test_name(test_name: str) -> str:
    cleaned = test_name.strip("\"").rstrip("()")
    m = method_matcher.match(cleaned)
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17767 extract test catalog from junit output [1/n] (#17397).

**Test**
No test added.
