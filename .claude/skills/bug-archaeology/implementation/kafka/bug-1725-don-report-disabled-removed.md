**Commit**: e546b0225b · **Ticket**: KAFKA-18307
**Subsystem**: .github/scripts
**Category**: Logic error in condition

**Root cause**
This patch ensures we only analyze trunk builds for the Develocity report. Some other minor fixes are also included

Reviewers: David Arthur <mumrah@gmail.com>

**Before/After**
```java
// Before
        return f'project:{project} buildStartTime:[{chunk_start.isoformat()} TO {chunk_end.isoformat()}] gradle.requestedTasks:{test_type}'
        remaining_build_ids: set,
        while continue_chunk and remaining_build_ids:
                        if build_id in remaining_build_ids:

    def get_build_info(self, build_ids: List[str], project: str, test_type: str, query_days: int) -> Dict[str, BuildInfo]:
        # Get builds from cache if available
        if self.build_cache:
            # Use cached data for builds within the cache period
            for build_id in build_ids:
                if build_id in cached_builds:
                    build = cached_builds[build_id]

// After
        return f'project:{project} buildStartTime:[{chunk_start.isoformat()} TO {chunk_end.isoformat()}] gradle.requestedTasks:{test_type} tag:github tag:trunk'
        remaining_build_ids: set | None,
        while continue_chunk and (remaining_build_ids is None or remaining_build_ids):
                        if remaining_build_ids is None or build_id in remaining_build_ids:
                                if remaining_build_ids is not None:
                                    remaining_build_ids.remove(build_id)
    def get_build_info(self, build_ids: List[str] = None, project: str = None, test_type: str = None, query_days: int = None, bypass_cache: bool = False, fetch_all: bool = False) -> Dict[str, BuildInfo]:
        current_time = datetime.now(pytz.UTC)
        if not fetch_all and not build_ids:
            raise ValueError("Either build_ids must be provided or fetch_all must be True")

        # Get builds from cache if available and bypass_cache is False
```

**How to spot**
When reviewing .github/scripts code, look for logic error in condition patterns. Check that all code paths handle edge cases and that the logic matches the documented contract. In particular: kafka-18307: don't report on disabled/removed tests (#18318).

**Test**
No test added.
