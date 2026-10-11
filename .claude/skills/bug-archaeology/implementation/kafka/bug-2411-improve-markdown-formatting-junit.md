**Commit**: ac4784ec0c · **Ticket**: KAFKA-17418
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
Newline characters in the failure message of tests were causing the Markdown tables to be malformed. This patch fixes that by replacing newlines with "<br>" tags and escaping other HTML that may appear in message. Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
// (no removals)

// After
import html
                if failure_message:
                    failure_message = html.escape(failure_message)
                    failure_message = failure_message.replace('\n', '<br>').replace('\r', '<br>')
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17418: improve markdown formatting in junit.py (#17071).

**Test**
No test added.
