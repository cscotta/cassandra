**Commit**: 1f3f03579c · **Ticket**: KAFKA-17979
**Subsystem**: tests/README.md
**Category**: Logic error in condition

**Root cause**
Reviewers: Chia-Ping Tsai <chia7712@gmail.com>

**Before/After**
```java
// Before
        $ python3 setup.py develop
[pytest]
from setuptools import find_packages, setup
from setuptools.command.test import test as TestCommand
class PyTest(TestCommand):
        TestCommand.initialize_options(self)
        TestCommand.finalize_options(self)
    def run_tests(self):
      install_requires=["ducktape==0.12.0", "requests==2.31.0", "psutil==5.7.2"],
      tests_require=["pytest", "mock"],
[pytest]

// After
        $ python3 -m pip install --editable .
[tool:pytest]
from setuptools import find_packages, setup, Command
class PyTest(Command):
    def run(self):
      install_requires=["ducktape==0.12.0", "requests==2.31.0", "psutil==5.7.2", "pytest==8.3.3", "mock==5.1.0"],
[tool:pytest]
```

**How to spot**
When reviewing tests/README.md code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-17979 change [pytest] to [tool:pytest] in setup.cfg file (#17740).

**Test**
Test modified in README.md covering the corrected behaviour.
