# Modernization Roadmap

Status: draft / proposal. Ordered by impact-to-effort. This is a sketch to argue
about, not a contract — phases are independent enough to reorder.

## Positioning (the north star)

Do **not** compete head-on as a general "compare two POJOs" library — AssertJ's
`usingRecursiveComparison()` and json-unit already own that. Lean into the two
things that are genuinely differentiated here:

1. A **structured, persistable diff** (`FeedbackNode` tree) instead of a thrown
   `AssertionError` — data you can store, aggregate, and diff across thousands of
   comparisons.
2. **Cross-field / indirect** matching (`IndirectMatcher`) — "field B should equal
   a transformation of a *different* field in A", which recursive-comparison
   comparators can't express.

Target use case: **validating structural equivalence at scale** — API migrations,
v1-vs-v2 contract checks, data-pipeline regression — where you want a *report*, not
a pass/fail.

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
- [~] JUnit: now runs on the **JUnit 5 Platform via the Vintage engine** (test
      sources are still JUnit 4). Full per-test migration to Jupiter is deferred —
      it's test-quality cleanup, not a build blocker. The one `@RunWith(Theories)`
      test needs conversion to `@ParameterizedTest` when migrated.
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

**Open follow-ups:** chains deeper than 4 hops (add more overloads or a fluent
builder if real models need it); typed paths can't yet express the `*` wildcard or
collection-index segments (`[key]`) — those remain string-only.

**Risk (resolved):** typed method-reference chains across nested generics get
verbose; prototyped via bounded arity overloads — landed as an *additional* API
alongside the string paths, not a replacement.

---

## Phase 3 — Make the feedback tree first-class (the actual moat)

This is what the library should be *known* for. Today `FeedbackNode` + `Json.mapper()`
exist but the story stops at "serialize one comparison."

- [ ] Aggregation: combine N comparison results into a summary (counts, top
      mismatching paths, per-field failure rates).
- [ ] A stable, documented JSON schema for the tree (it's the persistence format —
      treat it as an API with versioning).
- [ ] Human-readable report renderer (text/HTML), not just JSON.
- [ ] Query helpers: "all broken nodes", "mismatches under path X", filter/walk API.

**Done when:** comparing a corpus and getting an actionable summary report is a
one-liner.

---

## Phase 4 — Interop & distribution (drives adoption)

Meet people where they already are.

- [ ] AssertJ bridge: `assertThat(actual).matchesStructure(expected, spec)` so the
      tree can be used *inside* existing test suites (fail the test, but print the
      structured diff).
- [ ] JUnit 5 extension / assertion helpers.
- [ ] Publish to Maven Central (the `nl.alexeyu.structmatcher` group is already set).
- [ ] README rewrite around the new positioning; runnable `examples` updated to
      records + a batch/corpus scenario, not just the single bookstore compare.

---

## Phase 5 — Internal hardening (lower urgency)

- [ ] Reconsider the `ThreadLocal` `MatchingStackHolder`: it's global per-thread
      state for the duration of a `match()`. Works, but it's fragile and blocks any
      future parallel matching. Consider threading an explicit context object through
      the matcher tree instead.
- [ ] Generics cleanup: remove the `@SuppressWarnings("rawtypes")` /
      `instanceof IndirectMatcher` special-casing in `ContextAwareMatcher` if the
      type model can express it directly.
- [ ] Property-based tests for the wildcard path matcher (`WildcardPathChecker`) and
      list/order matchers.

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
