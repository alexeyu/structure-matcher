# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[semantic versioning](https://semver.org/).

## [Unreleased]

### Added

- **`e2e-test` module**, not published to Maven Central. One deployment-snapshot model runs
  through `core`, `json`, `report` and both assertion bridges: equivalent JSON and XML payloads
  match under a shared spec, the same regression in either format produces identical feedback,
  and a mixed batch persists as JSON Lines and rolls back up after a reload. The model covers
  each property shape core matches (bean and record, list, map, set, primitive array, `Optional`,
  enum), so it reaches the shapes the bookstore `examples` leave out. Its assertions compare
  ordered lists of broken paths, which pins the canonical ordering the archives depend on.

### Fixed

- **An accessor declared outside the library's reach is compared instead of dropped.** A getter a
  public model inherits from a package-private base reaches reflection only as a synthetic bridge,
  which property discovery filtered out, so the field was never compared and two objects differing
  in exactly that field were reported as matching. A bridge now survives when it is the only
  accessor of its name. The mirror case, a package-private class overriding a public supertype's
  accessor (what AutoValue and Immutables generate, and what a package-private record behind a
  public interface looks like), threw `IllegalAccessException`; the library now calls the nearest
  public supertype declaring the same signature. A bridge takes that route too where a supertype
  offers one, and an object no declaration fits is read through its own accessor of the same name,
  so two implementations of one interface can be compared with each other. The accessor is resolved
  when the property is read rather than when it is discovered, so an unreadable property can still
  be named, which is what a method reference to its accessor needs to register a matcher. Where
  nothing can be called, the new
  `InaccessibleAccessorException` names the accessor, the class and the remedy.
- **Map and set feedback no longer repeats a JSON key.** The nested rendering from `Json.mapper()`
  keys each child by its property name, and `SetMatcher`/`MapMatcher` name a node after the element
  or key `toString()`, so two distinct entries that print alike produced the same key twice and a
  reader kept only the last of them, losing a mismatch the tree had just been fixed to keep.
  Children that share a name now render once, as an array under that name.
- **Map and set feedback comes out in a fixed order.** A map or a set iterates in an order of its
  own, while a composite node compares its children in order, so the same entries filled in another
  order gave unequal feedback and differently ordered archives. Both matchers now sort their nodes
  by name, and by the node's own rendering when two keys or elements print alike.
- **The most specific registered path wins.** `WildcardMatcherResolver` streamed a `HashMap` and
  took the first match, so a wildcard rule plus an exact override for one field resolved by hash
  order and you could not influence it. Patterns now rank by fewest wildcards, then most named
  segments, then the longest run of names before the first wildcard, with the pattern text
  settling the rest. `PropertyPathPattern.MOST_SPECIFIC_FIRST` exposes the order.
- **A composite feedback node keeps every child.** `CompositeFeedbackNode` held its children in a
  `LinkedHashSet`, and `SetMatcher`/`MapMatcher` name a node after the element or key
  `toString()`, so two distinct entries that print alike collapsed into one and the report lost a
  mismatch. Children are now a `List`. Two nodes therefore compare equal only when their children
  match in the same order, which the property sorting below makes reproducible.
- **Bean properties are compared in name order.** Property discovery followed
  `Class.getMethods()`, whose order the JDK leaves unspecified, so the feedback tree, the JSON
  rendering and the persisted archives came out in a different order on another JDK build and two
  stored batches would not diff. Getters are now sorted by property name; record components keep
  their declaration order.

- **Collection properties declared with a concrete type are matched as collections.**
  `ClassProperty.isList`/`isMap`/`isSet` asked the assignability question backwards, so a property
  declared `ArrayList<String>` (or `HashMap`, `HashSet`) was matched as a structure instead, which
  compared the collection's own getters and reported differing collections as **matching**. Only
  interface-typed properties worked.
- **A type with no discoverable properties is no longer reported as matching.** Types the library
  cannot introspect (accessors without a `get`/`is` prefix, classes with only public fields, an
  array reached as a collection element) produced an empty feedback tree, i.e. "these match". The
  library now compares them by their own `equals`, or rejects them with the new
  `NoComparablePropertiesException` naming the type and property; registering a custom matcher for
  the property still overrides the default. A collection nested in a collection
  (`List<List<String>>`) is unaffected and stays the known limitation it was, since `ArrayList`
  does expose a property (`isEmpty()`).
- **Models implementing a generic interface no longer crash.** The same inverted check treated any
  accessor returning `Object` as a list, which is what the bridge method of a covariant accessor
  declares, so matching failed with `ClassCastException: class java.lang.String cannot be cast to
  class java.util.List`. Property discovery now skips bridge and synthetic methods, which also
  stops a property turning up twice.
- **A custom matcher may run a comparison of its own.** `ObjectMatcher.match` cleared the matching
  stack when it returned rather than putting back the one it found, so a matcher that ran a nested
  comparison left the outer one bare and every custom matcher registered after it was silently
  skipped. The nested comparison now restores the outer one.
- **Matching works on any thread.** The matching stack was seeded by a static initializer, which
  runs on one thread only, so a matcher used on any other thread hit a null stack and threw a
  `NullPointerException` with the origin masked. Every thread now starts from a bare stack, and a
  finished comparison drops its thread's entry instead of parking a placeholder there. Note that
  one comparison still has to run within one thread; a batch parallelizes across comparisons, not
  inside one.

## [2.0] - 2026-07-04

The first release published to Maven Central. Versions 1.x existed only as git tags
from 2017 and never reached a repository, so this is release one in practice.

Coordinates: `io.github.alexeyu:structure-matcher-<module>:2.0`. The Java packages remain
`nl.alexeyu.structmatcher.*`.

### Added

- **`record` support.** Property discovery reads record components, naming them as it names
  bean getters (`name()` and `getName()` both give the path segment `Name`), so a spec
  is the same whether the model uses records or classic beans.
- **`Map`, `Set`, array and `Optional` matching.** Maps compare by key and report missing
  and extra keys; sets compare by membership; arrays (object and primitive) adapt to lists;
  an `Optional` counts as a nullable value, with empty equivalent to `null`.
- **Typed accessor-chain paths.** `.with(matcher, BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip)`
  attaches a matcher through compiler-checked method references, up to four hops, so renaming an
  accessor updates the spec and the IDE completes each hop. String paths (with the `*` wildcard,
  and the only way to descend into collection elements) remain, and the two styles are
  interchangeable.
- **Fluent matcher composition.** `Matcher<V>` gained default methods, so matchers chain
  left to right like `Predicate` and `Comparator`:
  `nonNull().and(nonEmpty()).and(valuesEqual().normalizingBase(shorten))`.
- **`report` module** (no runtime dependencies). `FeedbackAggregator` rolls a batch of
  comparisons into a `FeedbackSummary`: totals, mismatch rate, and per-field failure counts
  and rates, ordered most-failing first. Collection indices collapse (`Books[0].Meta` and
  `Books[1].Meta` both count as `Books[].Meta`) and each field counts at most once per
  comparison, so a rate reads as "the fraction of comparisons in which this field broke".
  `FeedbackQuery` inspects a single tree: `brokenLeaves`, `find`, and `mismatchesUnder`.
- **Versioned JSON persistence** in the `json` module. `FeedbackArchives` reads and writes a
  flat, round-trippable archive (`{schemaVersion, matched, brokenLeaves:[{path, expectation, value}]}`).
  The reader rejects an unknown `schemaVersion` and ignores unknown fields. A whole batch
  persists as JSON Lines via `toJsonLines` / `fromJsonLines`.
- **Reload without re-comparing.** `FeedbackAggregator.addBrokenPaths` aggregates a stored
  comparison straight from its canonical paths, so a persisted batch rolls up without
  rebuilding any feedback trees.
- **`assertj` module.** `StructMatcherAssertions.assertThat(actual).matchesStructure(expected, spec)`
  fails with the per-field diff instead of two object dumps.
- **`junit5` module.** `StructAssertions.assertMatches(expected, actual, spec)`, with no AssertJ
  dependency. Throws opentest4j's `AssertionFailedError` carrying both objects, so the IDE
  comparison view still works.
- **`examples` module** with a runnable end-to-end bookstore scenario (XML vs JSON responses)
  demonstrating every consumer module.

### Changed

- **Java baseline is 17**; the build runs on Gradle 8.10.2 with a committed wrapper.
- Jackson 2.10.1 to 2.18.1; the whole test suite migrated to JUnit 5 (Jupiter), with no
  Vintage engine.
- `core` compiles clean under `-Xlint:unchecked,rawtypes -Werror`; the two casts that remain
  carry a javadoc explaining them, with the suppression scoped to the method.
- Spotless enforces import order and whitespace, checked in CI.

### Removed

- The varargs `Matchers.and(...)` static. Chain the fluent `matcher.and(other)` instead.

### Fixed

- `WildcardPathChecker` matched greedily and failed to backtrack, so it rejected a path such as
  `["A", "A"]` under a pattern that should match it.

[2.0]: https://github.com/alexeyu/structure-matcher/releases/tag/2.0
