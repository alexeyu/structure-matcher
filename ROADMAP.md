# Modernization Roadmap

Status: draft / proposal. Ordered by impact-to-effort. This is a sketch to argue
about, not a contract — phases are independent enough to reorder.

## Positioning (the north star)

Be honest about the competitive map — over-claiming loses the first knowledgeable
reader:

- A **structured, persistable POJO diff** on its own is **not** differentiated.
  JaVers and java-object-diff already produce serializable diff trees of arbitrary
  objects (and JaVers persists/queries them); jsondiffpatch/JSON Patch do it for
  JSON. Do not pitch "diff tree instead of a boolean" as the selling point.
- A general **"compare two POJOs"** library is owned by AssertJ's
  `usingRecursiveComparison()` and json-unit. Don't compete head-on there either.

The defensible niche is the **intersection** that none of those combine, for
arbitrary **nested** objects:

1. **Field-level tolerant / semantic rules** — not "are these equal" but "is this a
   valid IP", "within 2–5000ms", "equal after normalization", "order-insensitive".
   Equality-diff tools (JaVers, java-object-diff) can't express these as first-class
   rules.
2. **Cross-field / indirect** matching (`IndirectMatcher`) — "field B should equal a
   transformation of a *different* field in A". This is the rarest feature; no
   library surveyed (JVM, TS, Python, .NET) makes it first-class.
3. **A batch-level report** — per-field failure rates across thousands of
   comparisons, localizing *which* fields systematically diverge. `datacompy` proves
   the demand but only for flat DataFrames; `deepdiff` does tolerant nested diffs but
   no batch rollup.

One-line positioning that survives scrutiny: **"validate that two object streams are
*equivalent enough* under per-field rules, and get a report saying which fields
diverge and how often — across thousands of comparisons."** The report is the
product, not a debugging nicety.

Target use case: **equivalence validation at scale** — API v1-vs-v2 contract checks,
data-pipeline regression, cross-system reconciliation — where you want a *localized
report*, not a pass/fail. (Closest cousins by ecosystem: Python `deepdiff` for
tolerant nested diffing, `datacompy` for the per-field rate report, Jest asymmetric
matchers / `dirty-equals` for inline tolerant matchers — but none combine all three
for nested POJOs.)

---

## Phase 0 — Revive the build (blocker, low effort) — DONE (build green)

Nothing else can be merged or released until the project builds on a current
toolchain. Cosmetic for users, but a hard prerequisite.

- [x] Add a committed Gradle wrapper (was gitignored — fixed `.gitignore`,
      including a `!gradle/wrapper/gradle-wrapper.jar` exception to the `*.jar`
      rule). Now Gradle **8.10.2**.
- [x] Replace removed `maven` plugin and `compile`/`testCompile` with
      `maven-publish` and `api`/`implementation`/`testImplementation`. Dropped the
      incompatible sonarqube 2.5 plugin.
- [x] Bump Jackson (2.10.1 → 2.18.1) and Mockito (`mockito-all:1.10.19` →
      `mockito-core:5.14.2`, runner import updated).
- [x] JUnit: runs on the **JUnit 5 Platform**. Initially via the Vintage engine
      (test sources were still JUnit 4); the full per-test migration to Jupiter (incl.
      the `@RunWith(Theories)` → `@ParameterizedTest` conversion) landed later under the
      Phase 4 pre-publish release gate, and the Vintage engine was then dropped.
- [x] Java baseline set to **17 LTS** (toolchain). Bumping to 21 is a one-line
      change if/when desired.
- [x] CI: GitHub Actions workflow (`.github/workflows/build.yml`) runs
      `./gradlew build` on JDK 17 and uploads test reports. (Snapshot publishing
      to a repo is still open — needs credentials/target.)

**Done:** `./gradlew build` is green — 84 tests pass (78 core + 6 examples), 0
failures.

---

## Phase 1 — Close the capability gaps (high impact) — DONE

These are the "a 2026 developer bounces off it" issues. Each makes the library
viable for code people actually write today.

### 1a. Java `record` support (highest priority) — DONE
Records are the natural shape for the DTO/response payloads this library targets,
and **they didn't work**: `ClassProperty` only recognized `get`/`is`-prefixed
methods, but record accessors are `name()`, not `getName()` — so a record appeared
to have no properties and any two records silently "matched".
- [x] Generalize property discovery: `ClassProperty.forClass` detects
      `Class::isRecord` and derives properties from `getRecordComponents()`; getter-
      based discovery unchanged for classic POJOs.
- [x] Naming policy decided & unified: record component names are **capitalized**
      (`name()` → `Name`), identical to bean getters, so custom-matcher paths are
      the same whether a model uses records or beans. A `recordComponent` flag keeps
      a component literally named `getX` from being mis-stripped.
- [x] Tests: `RecordMatcherTest` mirrors `StructureMatcherTest` (identical feedback
      from record models) + path-based custom matcher on a nested record component;
      `ClassPropertyTest` covers record discovery/naming/values. 92 tests green.

### 1b. `Map`, `Set`, array support
Originally only simple values, `List`, and structures were handled. Modern models use
all three constantly.
- [x] `Map` matcher: `MapMatcher` compares by key, matches values with the default
      logic (recursing into structures), and reports missing/extra keys plus value
      mismatches under `property[key]`. Wired via `Property.isMap()` /
      `Matchers.forProperty` / `Matchers.mapsEqual()`. Tests: `MapMatcherTest` +
      `isMap` coverage in `ClassPropertyTest`.
- [x] `Set` matcher: `SetMatcher` matches by **membership** — same elements
      regardless of order, comparing elements by their own `equals`/`hashCode`,
      reporting missing/extra elements under `property[element]`. Wired via
      `Property.isSet()` / `Matchers.setsEqual()`. Tests: `SetMatcherTest` + `isSet`
      coverage. **Deviation from the original plan:** did *not* reuse
      `IgnoreOrderListMatcher` — it needs a `Comparator` (no default available) and a
      set's defining trait is membership, not sortable order. `IgnoreOrderListMatcher`
      stays as the comparator-based, field-aware option for *lists*.
- [x] Array matcher: `ArrayMatcher` adapts arrays to lists with reflection
      (`java.lang.reflect.Array`, so object **and primitive** arrays work) and
      delegates to `ListMatcher`. Wired via `Property.isArray()` /
      `Matchers.arraysEqual()`. Tests: `ArrayMatcherTest` (incl. `int[]`) + `isArray`
      coverage.

  Known follow-up (shared across all collection matchers): a collection
  *value/element* that is itself a collection (list-of-list, map-of-array, …) isn't
  deeply matched — `Matchers.forObject` routes non-simple values to
  `structuresEqual`, not to the collection matchers. Worth fixing once, centrally.

### 1c. `Optional` handling — DONE
- [x] `OptionalMatcher` treats `Optional<T>` as nullable `T` (empty ≈ null): it
      unwraps both sides and matches the contents with the default logic (two empties
      match; present-vs-empty does not). Wired via `Property.isOptional()` (an exact
      type check — `Optional` is final) / `Matchers.optional()`. Tests:
      `OptionalMatcherTest` + `isOptional` coverage.

**Done:** records, maps, sets, arrays, and `Optional` fields all match without
custom code. (Remaining cross-cutting follow-up: deep matching of *nested*
collections — list-of-list, map-of-array, etc. — tracked under 1b.)

---

## Phase 2 — API ergonomics (high impact, medium effort) — IN PROGRESS

The stringly-typed, capitalized paths (`"Server.Ip"`) have no compile-time safety
and break silently on rename. This is a real adoption friction and a place to beat
the competition.

- [x] Typed/lambda path API, e.g. `.with(matcher, BookSearchResult::getMetadata,
      SearchMetadata::getServer, Server::getIp)` — refactor-safe, IDE-completable.
      Implemented as **fixed-arity generic overloads** of `ObjectMatcher.with`
      (1–4 hops), so the whole chain is type-checked: each reference's return type
      must be the receiver of the next. A reference is a serializable
      `PropertyRef<T,R>` (`Function` + `Serializable`); `PropertyRefs.nameOf`
      recovers the accessor from the lambda's `SerializedLambda` and runs it through
      `ClassProperty.forMethod` — so a method reference produces *exactly* the same
      capitalized name a string path would (getter prefix stripped, record component
      capitalized). Zero new runtime deps. Tests: `PropertyRefsTest`,
      `TypedPathObjectMatcherTest`.
- [x] Keep string paths (incl. wildcards) as the dynamic/loosely-typed escape hatch.
      Untouched; typed and string registrations are interchangeable (same path
      strings) and a typed registration still honours wildcard string paths.
- [x] Reconsider the capitalized-path convention now that records are in play.
      **Decision: keep capitalization unified.** Because record components are
      already capitalized to match bean getters (Phase 1a), the typed API can reuse
      the identical naming rule and stay interchangeable with string paths — changing
      the convention now would split the two APIs for no gain.
- [x] Fluent matcher composition. Added default methods to the `Matcher<V>` functional
      interface (`and`, `normalizing`, `normalizingBase`, `normalizingBoth`) so matchers
      compose left-to-right like `Predicate`/`Comparator`, instead of inside-out static
      calls: `nonNull().and(nonEmpty()).and(valuesEqual().normalizingBase(...))`. The
      `Matchers.normalizing*(op, delegate)` statics now delegate to the defaults (one
      source of truth, back-compatible; the `and(...)` varargs factory stays). Made
      `IgnoreOrderListMatcher` public (its siblings already were, and a public factory
      returned it) so the fluent methods are reachable on its result. Tests:
      `MatcherCompositionTest`; adopted in the README, the `ObjectMatcher` javadoc, and
      the `ContextTolerantSpec` example.

**Open follow-ups:** chains deeper than 4 hops (add more overloads or a fluent
builder if real models need it); typed paths can't yet express the `*` wildcard or
collection-index segments (`[key]`) — those remain string-only.

**Risk (resolved):** typed method-reference chains across nested generics get
verbose; prototyped via bounded arity overloads — landed as an *additional* API
alongside the string paths, not a replacement.

---

## Phase 3 — Make the feedback tree first-class (the actual moat) — IN PROGRESS

This is what the library should be *known* for. Today `FeedbackNode` + `Json.mapper()`
exist but the story stops at "serialize one comparison."

- [x] Aggregation: combine N comparison results into a summary (counts, top
      mismatching paths, per-field failure rates). New **`report` module**
      (`nl.alexeyu.structmatcher.report`, `api project(':core')`) — kept out of core
      so the matching core stays a pure, zero-dep "produce feedback" library and the
      analysis surface (this, plus the renderer/query work below) can grow
      independently, mirroring how `json` is a separate consumer of the tree.
      `FeedbackAggregator` (incremental
      `add`/`addAll`/`summary`, plus a one-shot static `summarize`) produces a
      `FeedbackSummary` — total / matched / mismatched, `mismatchRate`, per-field
      failure counts and rates (ordered most-failing first), `topMismatchingFields`,
      and a readable `toString`. Built on `FeedbackPaths.brokenPaths`, which flattens
      a tree into registration-style paths (`Sub.Bool`, `Strings[0]`,
      `Books[0].Authors[0].FirstName`); `FeedbackPaths.toFieldPath` collapses
      `[index]`/`[key]`/`[element]` to `[]` so a field is counted once per comparison
      regardless of how many elements broke. Tests: `FeedbackPathsTest`,
      `FeedbackAggregatorTest`, `FeedbackReportEndToEndTest` (against a live
      `ObjectMatcher` run). Zero new deps.
- [x] A stable, documented JSON schema for the tree (it's the persistence format —
      treat it as an API with versioning). **Deviation from the literal item:** the
      existing `Json.mapper()` output is a nested, property-keyed *pretty rendering* —
      lossy (no node-type tag; `{}` is both a matched leaf and an empty composite; a
      model field named `expectation`/`value` collides with the leaf shape),
      write-only (no reader), and unversioned. Rather than freeze that as an API, the
      persistence format is a **separate, flat, versioned, round-trippable** shape in
      the `json` module: `FeedbackArchive` `{schemaVersion, matched, brokenLeaves:
      [{path, expectation, value}]}`, built on report's canonical paths
      (`FeedbackQuery.brokenLeaves`). `FeedbackArchives` reads/writes it: `toJson` /
      `archive` / `fromJson`, with `CURRENT_SCHEMA_VERSION = 1`; the reader rejects an
      unknown `schemaVersion` and ignores unknown fields (additive forward-compat).
      `json` now `implementation`-depends on `report` (no report type leaks into its
      public API). The nested `Json.mapper()` stays as the human-readable rendering
      (and groundwork for the text/HTML renderer below). Documented via javadoc on
      `FeedbackArchive` / `ArchivedLeaf` / `FeedbackArchives`. Tests: `FeedbackArchivesTest`
      (round-trip, version present, unsupported-version rejected, unknown-field
      tolerance, malformed-input rejection, one live `ObjectMatcher` run).
      Reload→aggregate is wired: `FeedbackAggregator.addBrokenPaths(Collection<String>)`
      (report) aggregates a comparison straight from its stored canonical paths — no
      `FeedbackNode` rebuild — and `FeedbackArchive.brokenPaths()` (json) supplies them.
      `add(FeedbackNode)` now delegates to `addBrokenPaths`, so live and reloaded
      corpora aggregate through one code path. report stays core-only (the bridge is
      plain strings, no json dependency). Tests: `FeedbackAggregatorTest`
      (`addBrokenPaths`), `ArchiveReloadAggregateTest` (json — persist → reload →
      aggregate yields the same summary as the live trees).
      **Follow-ups:** (a) **DONE** — batch persistence via JSON Lines:
      `FeedbackArchives.toJsonLines(feedbacks)` / `writeLines(archives)` write one
      compact archive per line (single-document output stays indented; the batch
      form uses a compact `ObjectWriter`), and `fromJsonLines(jsonl)` reads them
      back, skipping blank lines and validating each line's `schemaVersion` exactly
      as `fromJson`. So a whole batch persists to (or appends to) one document and
      reloads to roll up via `addBrokenPaths` — no report type leaks into `json`'s
      API (the bridge stays plain strings). Tests: `FeedbackArchivesTest` (round-trip,
      one-line-per-archive, empty batch, blank-line tolerance, bad-version-in-a-line
      rejection), `ArchiveReloadAggregateTest` (whole batch as one JSONL document →
      reload → same summary). (b) a user-facing `SCHEMA.md` can fold into the Phase 4
      README rewrite.
- [deferred] Human-readable report renderer (text/HTML), not just JSON. **Descoped for
      now** — JSON covers both jobs (the nested `Json.mapper()` rendering for reading one
      comparison, the flat versioned `FeedbackArchives` for persistence), and
      `FeedbackSummary.toString()` already gives a readable batch digest. A dedicated
      text/HTML renderer can be added on request; `FeedbackSummary`, `FeedbackPaths` and
      `FeedbackQuery` are the groundwork if/when it is.
- [x] Query helpers: "all broken nodes", "mismatches under path X", filter/walk API.
      New `FeedbackQuery` (report module, zero deps) returns `BrokenLeaf` records —
      each pairing a canonical path with the `ExpectationBroken` leaf, so callers get
      both *where* and *why* without re-walking: `brokenLeaves(tree)` (all broken
      nodes), `find(tree, Predicate<BrokenLeaf>)` (generic filter), and
      `mismatchesUnder(tree, "Books")` (segment-aware prefix — `Books` matches
      `Books[0].Title` but not `BooksCount`, and an exact leaf path fetches just that
      leaf). The tree traversal is now centralized in `FeedbackPaths.brokenLeaves`
      (package-private); `brokenPaths` maps over it, so path semantics stay in one
      place. `BrokenLeaf` also exposes `fieldPath()` / `expectation()` / `value()`.
      Tests: `FeedbackQueryTest` (hand-built trees + one live `ObjectMatcher` run).
      **Follow-up:** still returns flat lists, not a lazy walker over the *whole*
      tree (matched leaves and composites aren't surfaced); fine for the renderer
      below, revisit if a structural-walk use case appears.

**Done when:** comparing a batch and getting an actionable summary report is a
one-liner.

---

## Phase 4 — Interop & distribution (drives adoption)

Meet people where they already are.

- [x] AssertJ bridge: `assertThat(actual).matchesStructure(expected, spec)` so the
      tree can be used *inside* existing test suites (fail the test, but print the
      structured diff). New **`assertj` module** (`nl.alexeyu.structmatcher.assertj`,
      `api project(':core')` + `api assertj-core` + `implementation project(':report')`
      for the message) — a sibling consumer of the tree, like `json`/`report`, so core
      stays zero-dep. `StructMatcherAssertions.assertThat(actual)` returns a
      `StructureAssert<T>` (extends AssertJ's `AbstractAssert`) with two overloads:
      `matchesStructure(expected)` (default per-field equality — infers the class from
      `actual.getClass()`) and `matchesStructure(expected, spec)` (a caller-configured
      `ObjectMatcher` with tolerant / wildcard / typed-path rules). On mismatch it fails
      via `failWithMessage` with the structured per-field diff built from
      `FeedbackQuery.brokenLeaves` — every canonical path with its expected/actual value
      (e.g. `[Sub.Flag] expected: <true> but was: <false>`), so the failure localizes
      *which* fields diverged. `isNotNull()` guards a null actual. Tests:
      `StructureAssertTest` (pass, structured-diff failure, spec loosening the compare,
      null rejection) against a local record model. **Follow-up:** no soft-assertion
      / `assertSoftly` integration yet, and no navigation back into AssertJ (returns
      `this` for chaining but doesn't expose sub-field asserts).
- [x] JUnit 5 assertion helpers. New **`junit5` module**
      (`nl.alexeyu.structmatcher.junit5`, `api project(':core')` + `api opentest4j` +
      `implementation project(':report')`) — a sibling consumer of the tree, no AssertJ
      dependency, for the audience on plain JUnit assertions. `StructAssertions`
      (static): `assertMatches(expected, actual)` (default per-field equality — infers
      the class from the first non-null of the two; two nulls are trivially equivalent)
      and `assertMatches(expected, actual, spec)` (a caller-configured `ObjectMatcher`).
      On mismatch it throws opentest4j's `AssertionFailedError` carrying the two objects
      as expected/actual (so a JUnit 5 IDE offers its comparison view) and a message
      listing each broken `FeedbackQuery.brokenLeaves` path with its expected/actual
      value. `AssertionFailedError` is JUnit 5's assertion foundation but engine-agnostic,
      so the helpers work under JUnit 5 or 4 (the module's own tests are JUnit 4 via
      Vintage, per repo convention). Tests: `StructAssertionsTest` (pass, throw-with-
      expected/actual-and-diff, spec loosening, two-nulls) against a local record model.
      **Deviations/notes:** descoped a Jupiter `@ExtendWith` *extension* (parameter
      resolver / lifecycle hook) — the static helpers cover the assertion use case with
      no engine coupling; add an extension on demand. opentest4j's `ValueWrapper.getValue()`
      only retains `Serializable` values, so for non-serializable models the objects ride
      on `getEphemeralValue()` + a string representation (what the IDE renders) — the
      test asserts via `getEphemeralValue()`.
### Release gate: pre-publish polish (do these before Maven Central)

A punch-list to finish before the first public `2.0` release — the artifacts are
permanent once on Central, so tighten the surface first.

- [x] **Showcase AssertJ & JUnit 5 in `examples`.** Added `AssertJExampleTest`
      (`assertThat(actual).matchesStructure(expected, spec)`) and `JUnitAssertionExampleTest`
      (`StructAssertions.assertMatches(expected, actual, spec)`) in the bookstore scenario
      alongside `ResponseMatchingTest` / `BatchReportTest`, so `examples` now depends on
      and demonstrates every consumer module (`json`, `report`, `assertj`, `junit5`). Each
      shows the real value prop — a v1-prod response asserted *equivalent enough* to the v1
      test baseline under a tolerant spec (dynamic IP regex, port pool, timing range) — plus
      the structured per-field diff a raw comparison produces (AssertJ: `assertThatException…
      withMessageContaining`; JUnit: `assertThrows(AssertionFailedError…)` asserting the
      diverging paths and the expected/actual objects ride along via `getEphemeralValue()`).
      Wired `testImplementation project(':assertj' / ':junit5')` (AssertJ / opentest4j arrive
      transitively as their `api` deps). **Note:** the example models were *already* records,
      so the record-showcase follow-up under the README item is effectively done.
- [x] **Migrate the test suite from JUnit 4 to JUnit 5 (Jupiter).** Done — all test
      classes across all six modules now run on Jupiter; the `junit-vintage-engine` and
      `junit:junit:4.13.2` dependencies are dropped from the root `build.gradle`. Bulk
      conversions: `org.junit.Test`→`org.junit.jupiter.api.Test`,
      `@Before`→`@BeforeEach`, `org.junit.Assert.*`→`org.junit.jupiter.api.Assertions.*`
      (with the message-argument flip on the ~6 message-carrying asserts, e.g.
      `assertTrue(msg, cond)`→`assertTrue(cond, msg)`), and `org.junit.Assert.assertThat`
      (deprecated) → `org.hamcrest.MatcherAssert.assertThat` in `ResponseMatchingTest`.
      Special cases: five `@Test(expected = X.class)` → `assertThrows` (IntegerMatcherTest,
      PropertyPathTest ×2, PropertyPathPatternTest ×2); `ContextAwareMatcherTest`
      `@RunWith(MockitoJUnitRunner.class)` → `@ExtendWith(MockitoExtension.class)` (added
      `mockito-junit-jupiter:5.14.2`, strict stubs pass unchanged); `WildcardPathCheckerTest`
      `@RunWith(Theories.class)` / `@Theory` / `@DataPoints` → `@ParameterizedTest` +
      `@MethodSource` (the data-point arrays become method sources — now 15 individually
      reported cases). Migrated module-by-module (both engines coexisted during the pass),
      each verified with `:<module>:test`; final `clean build` green: **218 tests, 0
      failures**. This closes the long-deferred Phase 0 `[~]` JUnit item.
- [x] **Generics cleanup** (moved up from Phase 5). Removed the
      `@SuppressWarnings("rawtypes")` / raw-`IndirectMatcher` `instanceof` special-casing
      from the hot matching code (`Matchers.forProperty`, `ContextAwareMatcher`) so the
      main sources compile without unchecked/rawtypes warnings, localizing any truly
      unavoidable erasure to a single documented spot. **Done:** `ContextAwareMatcher` now
      uses a generic `instanceof IndirectMatcher<?, ?>` pattern; no `rawtypes` suppression
      remains anywhere in `core/src/main`; the only two casts are documented
      `@SuppressWarnings("unchecked")` boundaries (`IndirectMatcher.matchStructures`,
      `Matchers.asObjectMatcher`), each javadoc'd with why the erasure is safe. A
      `-Xlint:unchecked,rawtypes -Werror` guard on `:core:compileJava` locks it in so it
      can't silently regress.
- [x] **Randomized tests** (moved up from Phase 5) for `WildcardPathChecker` and the
      list/order matchers (`ListMatcher`, `IgnoreOrderListMatcher`), complementing the
      example-based tests with generated inputs (structural properties of wildcard
      matching; list reflexivity/size-sensitivity; order-insensitive permutation
      invariance). **Done:** covered by `WildcardPathCheckerRandomizedTest` and
      `ListMatcherRandomizedTest` — JUnit 5 `@ParameterizedTest`/`@MethodSource` over
      fixed-seed randomized inputs plus explicit edge cases (incl. the `["A", "A"]`
      wildcard-backtracking regression, which is what first caught the greedy-match bug in
      `WildcardPathChecker`, now fixed). Went with randomized JUnit tests rather than a
      property-based framework: jqwik ≥1.10 ships an anti-AI `stdout` banner, so we dropped
      it and forgo automatic shrinking — compensating with a fixed seed (reproducible
      failures) and hardcoded edge cases.
- [x] **Solid README.** Leads with the narrow niche (equivalence validation at scale
      with a per-field report) and says what it is not: a general object diff (JaVers,
      java-object-diff) or a single-object assertion. **Done:** full top-to-bottom
      rewrite. New niche-first intro, a quick-start, a module map (core, json, report,
      assertj, junit5), a "using it in tests" section for the assertj/junit5 bridges, and
      the batch-report and persistence sections (`FeedbackAggregator`/`FeedbackSummary`/
      `FeedbackQuery`, the two `json` shapes, and a runnable `BatchReportTest`), now with
      a realistic batch example that replays a set of queries against both APIs. Concise
      style pass throughout.
- [ ] **Critical code & comment polish pass.** Re-read the whole surface with fresh
      eyes now that six modules exist: public-API javadoc completeness and honesty,
      dead/duplicated code (e.g. the near-identical broken-leaf message formatting now
      living in both `assertj` and `junit5` — decide whether to hoist a shared renderer
      into `report` or keep them intentionally distinct), naming consistency, and
      comments that have drifted from the code. Tighten before the API is frozen by a
      public release.
- [ ] **Bump version `1.1-SNAPSHOT` → `2.0`.** Single point of change in the root
      `build.gradle` (`version = '1.1-SNAPSHOT'`). Do this last, as the release commit,
      once the above land. (Major bump is justified: records/maps/sets/arrays/Optional,
      typed paths, the report + json-archive + assertj + junit5 modules are all new
      since the last line.)

- [ ] Publish to Maven Central (the `nl.alexeyu.structmatcher` group is already set) —
      **gated on the release-gate checklist above.**

---

## Phase 5 — Internal hardening (lower urgency)

- [ ] Reconsider the `ThreadLocal` `MatchingStackHolder`: it's global per-thread
      state for the duration of a `match()`. Works, but it's fragile and blocks any
      future parallel matching. Consider threading an explicit context object through
      the matcher tree instead.

---

## Suggested sequence

```
Phase 0  ──▶  Phase 1 (records first)  ──▶  Phase 3  ──▶  Phase 4
                     │                          ▲
                     └──▶ Phase 2 ──────────────┘     Phase 5 anytime after 0
```

Ship 0 + 1a as the first milestone (a build that works + records) — that alone
moves the library from "won't run on my project" to "I can try it." Phases 3–4 are
what make others actually *choose* it.
