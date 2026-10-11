## Commit
e825d6ae5b

## Ticket
KAFKA-20299

## Subsystem
Testing infrastructure / Share Groups

## Category
State not cleaned up (obsolete config)

## Root cause
`use_share_groups` was a temporary parameter added when share groups were experimental. In AK 4.2 the `group.share.enable` config was removed and share groups became enabled by default. The parameter had been propagated through `KafkaService`, broker templates, and test matrices, but was now dead: its only effect was setting `share.coordinator.state.topic.*` configs that should always be set.

## Before
```python
@matrix(..., use_share_groups=[True])
def test_lifecycle(self, ..., use_share_groups=True):
    self.kafka = KafkaService(..., use_share_groups=use_share_groups)
```
```jinja
{% if use_share_groups is not none and use_share_groups %}
share.coordinator.state.topic.replication.factor=...
{% endif %}
```

## After
```python
@matrix(...)
def test_lifecycle(self, ...):
```
```properties
# Always set in kafka.properties
share.coordinator.state.topic.replication.factor=...
```

## How to spot
Feature-flag parameters that can only be `True` (no test ever passes `False`) are dead. Search for `use_share_groups=False` in the test corpus—if absent, the flag is always on and should be removed.

## Test
All share group system tests continue to pass without the parameter.
