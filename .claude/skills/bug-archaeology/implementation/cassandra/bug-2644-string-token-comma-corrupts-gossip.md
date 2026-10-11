# Bug 2644: String tokens containing commas corrupt gossip VersionedValue because comma is the gossip delimiter

**Commit**: 6a9a336ad4 · **Ticket**: CASSANDRA-2762

## Detection Rule
When validating a token string at startup or assignment, reject any value that contains the gossip VersionedValue delimiter character (comma), and add a `validate()` method to each partitioner's token factory to enforce partitioner-specific token format rules.

## Code Smell
`OrderPreservingPartitioner` and other partitioners accept arbitrary string tokens from configuration without checking that they are free of the gossip delimiter; a token string containing a comma is valid from a partitioner standpoint but corrupts the gossip `VersionedValue` when serialized, causing all nodes to misparse ring state.

## Tags
- scope: cassandra
- category: wrong-serialization
- code-pattern: missing-input-validation-delimiter
- severity: crash
- subsystem: gossip

## Root Cause
Gossip encodes ring state in `VersionedValue` strings using comma as a delimiter between fields. The `OrderPreservingPartitioner` accepts arbitrary UTF-8 strings as tokens but never validated that they do not contain a comma. If a user configured `initial_token` with a comma-containing string, the gossip subsystem would split the encoded value at the comma, causing all nodes to misparse the token, leading to wrong ring topology. The fix adds a `validate()` abstract method to `Token.TokenFactory` and implements it in each partitioner to catch invalid tokens at startup.

## Example
```java
// BEFORE – no validation of initial_token at startup
if (conf.initial_token != null)
    ; // nothing checked

// AFTER
if (conf.initial_token != null)
    partitioner.getTokenFactory().validate(conf.initial_token);

// OrderPreservingPartitioner TokenFactory.validate():
public void validate(String token) throws ConfigurationException {
    if (token.contains(VersionedValue.DELIMITER_STR))
        throw new ConfigurationException("Tokens may not contain the character " + VersionedValue.DELIMITER_STR);
}
```
