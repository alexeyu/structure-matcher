# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project follows
[semantic versioning](https://semver.org/).

## [2.0] - 2026-07-04

The first release published to Maven Central. Versions 1.x existed only as git tags
from 2017 and were never distributed, so for practical purposes this is release one.

Coordinates: `io.github.alexeyu:structure-matcher-<module>:2.0`. The Java packages remain
`nl.alexeyu.structmatcher.*`.

### Added

- **`record` support.** Record components are discovered as properties, named identically
  to bean getters (`name()` and `getName()` both give the path segment `Name`), so a spec
  is the same whether the model uses records or classic beans.
- **`Map`, `Set`, array and `Optional` matching.** Maps compare by key and report missing
  and extra keys; sets compare by membership; arrays (object and primitive) adapt to lists;
  `Optional` is treated as a nullable value, with empty equivalent to `null`.
- **Typed accessor-chain paths.** `.with(matcher, BookSearchResult::getMetadata, SearchMetadata::getServer, Server::ip)`
  attaches a matcher through compiler-checked method references, up to four hops. Refactor-safe
  and IDE-completable. String paths (with the `*` wildcard, and the only way to descend into
  collection elements) remain, and the two styles are interchangeable.
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
- `core` compiles clean under `-Xlint:unchecked,rawtypes -Werror`; the two unavoidable casts
  are documented rather than blanket-suppressed.
- Spotless enforces import order and whitespace, checked in CI.

### Removed

- The varargs `Matchers.and(...)` static. Chain the fluent `matcher.and(other)` instead.

### Fixed

- `WildcardPathChecker` matched greedily and failed to backtrack, so a path such as `["A", "A"]`
  could be rejected by a pattern that should match it.

[2.0]: https://github.com/alexeyu/structure-matcher/releases/tag/2.0
