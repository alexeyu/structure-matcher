# How this compares to JaVers, AssertJ and json-unit

Comparing two Java objects is a solved problem several times over. This page is an honest map of
who solves which version of it, including the cases where you should use something else.

## The short version

| You want to… | Use |
|---|---|
| Assert one object matches an expected one in a test, ignoring or loosening a few fields | **AssertJ** `usingRecursiveComparison()` |
| Track how an object changed over time, with an audit repository you can query | **JaVers** |
| Compare two JSON documents with placeholders and regexes | **json-unit** |
| Check that two *streams* of objects stay equivalent under per-field rules, and get a report of which fields diverge and how often | **structure-matcher** |

If your problem is the first row, use AssertJ. It is in your test classpath already, it is
maintained by a large team, and for a single assertion it does the job well. Structure matcher
earns its place when the comparison result is the product rather than a pass/fail.

## AssertJ recursive comparison

The closest overlap, and more capable than it is often given credit for. It supports per-field
custom equality (`withEqualsForFields(BiPredicate, "field")`), per-type comparators
(`withComparatorForType`), field and regex exclusions (`ignoringFields`,
`ignoringFieldsMatchingRegexes`), null tolerance (`ignoringActualNullFields`) and
order-insensitive collections (`ignoringCollectionOrder`, `ignoringCollectionOrderInFields`).

Where AssertJ and structure-matcher diverge:

- **The result.** The fluent API is an *assertion*: it succeeds or fails with a message. AssertJ
  can also hand you the differences as data, though the route is not part of the documented
  assertion surface: `new RecursiveComparisonDifferenceCalculator().determineDifferences(actual,
  expected, config)` is public and returns a `List<ComparisonDifference>`. What AssertJ does not
  give you is anywhere to *put* them: no stable serialization format, no reload path.
- **Across many comparisons.** AssertJ has no notion of a batch. Structure matcher's `report`
  module rolls N comparisons into per-field failure rates, and `json` persists each result in a
  versioned format that reloads and aggregates without re-comparing. This, not the diff tree
  itself, is the real difference, and it only matters if you are reconciling two systems rather
  than checking one object.
- **Cross-field rules.** `IndirectMatcher` compares a field against a *different* field of the
  other object (an initial derived from a full first name, say). AssertJ's per-field predicates
  receive only the two values for that field.
- **Where it runs.** AssertJ is a test-scope assertion library. Structure matcher's `core` and
  `report` have no runtime dependencies and are meant to run in production reconciliation jobs too.

Summary: **for a single test assertion, AssertJ is the better tool** Structure matcher overtakes 
it only when comparisons pile up: when you need to store thousands of them and answer 
"which fields have been drifting".

## JaVers

JaVers diffs arbitrary objects into a `Diff` of `Change` objects, serializes it to JSON, and, with
its repository, stores object history so you can query how an entity changed over time. That audit
story is something structure-matcher does not attempt.

Differences that matter for the equivalence-checking use case:

- JaVers customization is registered **per type** (`registerCustomComparator(comparator, Multimap.class)`,
  `CustomValueComparator` for value types). Structure matcher binds rules to a **path**
  (`Metadata.Server.Ip`, or a `*` wildcard, or a typed accessor chain), so two `String` fields in
  the same object can have different rules.
- JaVers answers "what changed between these two objects". Structure matcher answers "does this
  object satisfy the rules I expect of it, and if not, where" - a validation question, not a
  changelog one.
- No batch rollup of per-field failure rates across many comparisons.

If you want an audit trail of a domain object's history, use JaVers.

## java-object-diff

Produces a node tree of differences with inclusion and exclusion rules. A reasonable design; the
latest release on Maven Central is 0.95. Equality-based, with no first-class notion of a tolerant
rule per field.

## json-unit

Excellent at what it does: comparing two JSON documents with placeholders, regexes and custom
matchers. If your data is already JSON and you never need it as objects, prefer it. Structure
matcher works on POJOs and records, so it applies before or after serialization, and to sources
(a database row mapped to a record, a gRPC response) that never pass through JSON.

## Outside the JVM

`deepdiff` (Python) does tolerant nested diffs; `datacompy` (Python) produces the per-column
mismatch report but only for flat DataFrames; Jest asymmetric matchers and `dirty-equals` give
inline tolerant matchers. The combination of tolerant per-field rules **and** a batch rollup for
**nested** objects is what none of them offer together.

## Where structure-matcher is weak

- **Young and barely used.** Version 2.0 is the first release on Maven Central. There is no
  large production track record behind it.
- **Nested collections are not deeply matched.** A list of lists, or a map of arrays, routes its
  elements to structure matching rather than back to the collection matchers. Tracked in the
  roadmap.
- **Typed accessor chains stop at four hops**, and cannot express the `*` wildcard or collection
  index segments. Those paths remain strings, which are not refactor-safe.
- **Matching relies on thread-local state** for the duration of a `match()` call, so a single
  comparison must stay on one thread.
- **Reflection over getters and record components**, so fields without accessors are invisible.
