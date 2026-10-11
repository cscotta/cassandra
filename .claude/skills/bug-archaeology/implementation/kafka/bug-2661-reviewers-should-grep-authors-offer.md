**Commit**: fdee225a1b · **Ticket**: KAFKA-17177
**Subsystem**: reviewers.py
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
    stream = os.popen("git log | grep Reviewers")
        stripped = line.strip().lstrip("Reviewers: ")
        m = re.match("(?P<name>.*)\s<(?P<email>.*)>", item[0])

// After
    command = r"git log | grep 'Reviewers\|Author'"
    stream = os.popen(command)
        stripped = line.strip().lstrip("Reviewers: ").lstrip("Author: ")
        patterns = r"(?P<name>.*)\s<(?P<email>.*)>"
        m = re.match(patterns, item[0])
```

**How to spot**
When reviewing reviewers.py code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17177 reviewers.py should grep "authors" to offer more candidates of reviewers information (#16674).

**Test**
No test added.
