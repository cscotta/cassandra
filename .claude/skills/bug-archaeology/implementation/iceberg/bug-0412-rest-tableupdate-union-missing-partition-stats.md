# Bug 0412: SetPartitionStatisticsUpdate and RemovePartitionStatisticsUpdate missing from REST TableUpdate union

**Commit**: f03595376978fb6c3fad4c374a76507ca0bb87e9 · **Ticket**: #15115

## Detection Rule

When a discriminated union type (oneOf/anyOf in OpenAPI, sealed class, or tagged union) has named subtypes defined elsewhere in the schema, verify that every defined subtype is listed in the union — grep for the subtype name and confirm it appears in both the definition block and the union membership list.

## Code Smell

An OpenAPI schema defines a `TableUpdate` `oneOf` union and separately defines `SetPartitionStatisticsUpdate` and `RemovePartitionStatisticsUpdate` components, but the union list does not contain `$ref` entries for these components; generated client models therefore do not parse them as valid `TableUpdate` variants.

## Tags

- scope: project-specific
- category: wrong-serialization
- code-pattern: union-missing-member
- severity: silent-wrong-result
- subsystem: rest-catalog/open-api-spec

## Root Cause

The OpenAPI spec (`rest-catalog-open-api.yaml`) and its generated Python model omitted `SetPartitionStatisticsUpdate` and `RemovePartitionStatisticsUpdate` from the `TableUpdate` `oneOf` union even though both types were fully defined in the components section. Any REST client that deserializes a `TableUpdate` payload containing one of these variants would fail to recognize it, leading to parse errors or silent data loss depending on the consumer's behavior.

## Example

```yaml
# BEFORE — union missing two partition statistics update variants
TableUpdate:
  oneOf:
    - $ref: '#/components/schemas/SetStatisticsUpdate'
    - $ref: '#/components/schemas/RemoveStatisticsUpdate'
    # SetPartitionStatisticsUpdate and RemovePartitionStatisticsUpdate absent
    - $ref: '#/components/schemas/RemovePartitionSpecsUpdate'
```

```yaml
# AFTER — both variants added to union
TableUpdate:
  oneOf:
    - $ref: '#/components/schemas/SetStatisticsUpdate'
    - $ref: '#/components/schemas/RemoveStatisticsUpdate'
    - $ref: '#/components/schemas/SetPartitionStatisticsUpdate'
    - $ref: '#/components/schemas/RemovePartitionStatisticsUpdate'
    - $ref: '#/components/schemas/RemovePartitionSpecsUpdate'
```
