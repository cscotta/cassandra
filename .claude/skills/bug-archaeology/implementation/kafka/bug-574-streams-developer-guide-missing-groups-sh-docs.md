**Commit**: c274374743 · **Ticket**: KAFKA-19380

**Subsystem**: docs/streams/developer-guide/

**Category**: Missing null/bounds check

**Root cause**
The Streams developer guide documented the `kafka-streams-application-reset.sh` tool without mentioning the related `kafka-streams-groups.sh` CLI commands that are used for group inspection and management. A user following the documentation would have no way to discover the group-management tool. The fix creates a new `kafka-streams-group-sh.html` page listing all available CLI commands and adds a reference to it from both `app-reset-tool.html` and `index.html`.

**Before**
```html
<!-- Before: app-reset-tool.html made no reference to kafka-streams-groups.sh -->
```

**After**
```html
<!-- After: new page linked from app-reset-tool.html and index.html -->
<a href="kafka-streams-group-sh.html">kafka-streams-groups.sh Commands</a>
```

**How to spot**
Check the Streams developer-guide index for entries that cover administrative CLI tools; any tool that exists in `bin/` but is absent from the developer-guide index is a documentation gap.

**Test**
Load the updated HTML in a browser and confirm the new `kafka-streams-group-sh.html` page is reachable from both the index and the app-reset-tool page.
