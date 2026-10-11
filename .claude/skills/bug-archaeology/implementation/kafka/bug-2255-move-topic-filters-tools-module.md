**Commit**: 8dbbf5ea76 · **Ticket**: KAFKA-14705
**Subsystem**: checkstyle/import-control.xml
**Category**: Logic error in condition

**Root cause**
After MirrorMaker 1 removal, there are no other modules dependencies for these classes, so we can safely move them to tools module. Signed-off-by: Federico Valeri <fedevaleri@gmail.com>
Reviewers: Mickael Maison <mickael.maison@gmail.com>

**Before/After**
```java
// Before
import org.apache.kafka.server.util.PartitionFilter;
import org.apache.kafka.server.util.PartitionFilter.PartitionRangeFilter;
import org.apache.kafka.server.util.PartitionFilter.PartitionsSetFilter;
import org.apache.kafka.server.util.PartitionFilter.UniquePartitionFilter;
import org.apache.kafka.server.util.TopicFilter.IncludeList;
import org.apache.kafka.server.util.TopicPartitionFilter;
import org.apache.kafka.server.util.TopicPartitionFilter.CompositeTopicPartitionFilter;
import org.apache.kafka.server.util.TopicPartitionFilter.TopicFilterAndPartitionFilter;
import org.apache.kafka.server.util.TopicFilter;
import org.apache.kafka.server.util.TopicFilter.IncludeList;
package org.apache.kafka.server.util;
package org.apache.kafka.server.util;

// After
    <allow pkg="org.apache.kafka.tools.filter" />
import org.apache.kafka.tools.filter.PartitionFilter;
import org.apache.kafka.tools.filter.PartitionFilter.PartitionRangeFilter;
import org.apache.kafka.tools.filter.PartitionFilter.PartitionsSetFilter;
import org.apache.kafka.tools.filter.PartitionFilter.UniquePartitionFilter;
import org.apache.kafka.tools.filter.TopicFilter.IncludeList;
import org.apache.kafka.tools.filter.TopicPartitionFilter;
import org.apache.kafka.tools.filter.TopicPartitionFilter.CompositeTopicPartitionFilter;
import org.apache.kafka.tools.filter.TopicPartitionFilter.TopicFilterAndPartitionFilter;
import org.apache.kafka.tools.filter.TopicFilter;
import org.apache.kafka.tools.filter.TopicFilter.IncludeList;
package org.apache.kafka.tools.filter;
```

**How to spot**
When reviewing checkstyle/import-control.xml code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-14705: move topic filters to tools module (#17387).

**Test**
Test modified in GetOffsetShellParsingTest.java covering the corrected behaviour.
