**Commit**: 4e551de6c3 · **Ticket**: KAFKA-19039
**Subsystem**: committer-tools/refresh_collaborators.py
**Category**: Wrong serialization / deserialization

**Root cause**
`update_local_yaml_content` in `refresh_collaborators.py` instantiates a
YAML object with no arguments. As such, the YAML object defaults to
using a `RoundTripRepresenter` under the hood, which serializes `None`
into an empty string. This PR updates the YAML object's serialization behaviour to return
`"~"` for `None`. Reviewers: Lianet Magrans <lmagrans@confluent.io>, Ming-Yen Chung
 <mingyen066@gmail.com>

**Before/After**
```java
// Before
from typing import Dict, List, Tuple

// After
from typing import Any, Dict, List, Tuple
from ruamel.yaml.representer import RoundTripRepresenter
    def represent_none(self: RoundTripRepresenter, _: Any):
        return self.represent_scalar("tag:yaml.org,2002:null", "~")

        yaml.representer.add_representer(type(None), represent_none)
```

**How to spot**
When reviewing committer-tools/refresh_collaborators.py code, look for wrong serialization / deserialization patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-19039: the refresh_collaborators.py script messes .asf.yaml (#19459).

**Test**
No test added.
