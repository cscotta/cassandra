# Bug 0233: Hadoop Configuration serialized via binary protocol includes inherited default entries

**Commit**: ba403009d2636e1f422a865ebae083e77aa443ad · **Ticket**: #15583

## Detection Rule

When a class wraps a Hadoop `Configuration` and implements Java `Serializable`, verify that serialization captures only the explicitly-set key/value pairs as a plain `Map<String, String>` rather than delegating to Hadoop's own `DataInput`/`DataOutput` binary protocol, which pulls in entries inherited from default resources.

## Code Smell

A `Serializable` wrapper class with `writeObject(ObjectOutputStream)` that calls `hadoopConf.write(out)` and a matching `readObject` that calls `hadoopConf.readFields(in)`; any unintended entries sourced from Hadoop default resources round-trip into the deserialized configuration.

## Tags

- scope: java
- category: wrong-serialization
- code-pattern: binary-protocol-leaks-defaults
- severity: silent-wrong-result
- subsystem: core/hadoop/SerializableConfiguration

## Root Cause

`SerializableConfiguration` used Hadoop's `Configuration.write(DataOutput)` / `readFields(DataInput)` methods, which serialize the configuration using Hadoop's own binary format. This format iterates all entries including those inherited from default resources, causing the deserialized object to contain more keys than the caller set. Additionally, the serialized form is tied to Hadoop's internal wire format and not standard Java serialization, creating a hidden cross-version compatibility hazard. The fix captures only the explicitly-iterated entries into a `Map<String, String>` at construction time and reconstructs a clean `Configuration(false)` from that map on deserialization.

## Example

```java
// BEFORE — Hadoop binary protocol; includes default-resource entries, tied to Hadoop internals
private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    hadoopConf.write(out);      // Hadoop DataOutput format
}
private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
    hadoopConf = new Configuration(false);
    hadoopConf.readFields(in);  // reconstructs with all entries Hadoop wrote
}
```

```java
// AFTER — plain Map<String,String>; only explicitly-set entries; standard Java serialization
public SerializableConfiguration(Configuration hadoopConf) {
    this.confAsMap = Maps.newHashMapWithExpectedSize(hadoopConf.size());
    hadoopConf.forEach(entry -> confAsMap.put(entry.getKey(), entry.getValue()));
}
@Override
public Configuration get() {
    if (hadoopConf == null) {
        Configuration newConf = new Configuration(false);
        confAsMap.forEach(newConf::set);
        this.hadoopConf = newConf;
    }
    return hadoopConf;
}
```
