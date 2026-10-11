**Commit**: bb7c083049 · **Ticket**: KAFKA-17781
**Subsystem**: tests/docker
**Category**: Wrong constant / default value

**Root cause**
The system test Docker image pinned ducktape to version 0.11.4 and did not list `psutil` as an explicit dependency. When ducktape was upgraded to 0.12.0, `psutil` became a required but unlisted transitive dependency, causing environment setup failures. The version pin was a stale value that did not track the release that introduced the new requirement.

**Before/After**
```python
# Before
ducktape==0.11.4
# (no psutil entry)

# After
ducktape==0.12.0
psutil==5.9.8
```

**How to spot**
When upgrading a pinned dependency in a requirements file, search for new mandatory transitive dependencies introduced in the new version's changelog or setup.py. If the Docker image or CI environment omits them, installation will succeed but runtime imports will fail with `ModuleNotFoundError`.

**Test**
No test added.
