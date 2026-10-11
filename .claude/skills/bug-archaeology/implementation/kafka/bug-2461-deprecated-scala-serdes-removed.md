**Commit**: 4b5df1f8e9 · **Ticket**: KAFKA-12826
**Subsystem**: streams/streams-scala/Serdes
**Category**: State not cleaned up

**Root cause**
`org.apache.kafka.streams.scala.Serdes` was deprecated in 2.7.0 in favour of `org.apache.kafka.streams.scala.serialization.Serdes`. Keeping the deprecated class in the codebase caused confusion because two identically-named objects existed in sibling packages. Users upgrading to 4.0 who had explicit imports of the deprecated class would encounter subtle resolution ambiguities. Removing it forces a clean migration.

**Before/After**
```scala
// Before (deprecated, duplicate of serialization.Serdes)
@deprecated("Use org.apache.kafka.streams.scala.serialization.Serdes", "2.7.0")
object Serdes {
  implicit def String: Serde[String] = JSerdes.String()
  implicit def Long: Serde[Long] = JSerdes.Long().asInstanceOf[Serde[Long]]
  ...
}

// After
// class removed; users must use serialization.Serdes
```

**How to spot**
When two Scala `object`s or Java classes with the same unqualified name exist in sibling packages within the same module, reviewers should check whether one is a deprecated alias of the other. If the deprecation deadline has passed, remove the older variant to eliminate the ambiguity.

**Test**
No test added.
