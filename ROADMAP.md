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

## Phase 1 — Close the capability gaps (high impact)

These are the "a 2026 developer bounces off it" issues. Each makes the library
viable for code people actually write today.

### 1a. Java `record` support (highest priority)
Records are the natural shape for the DTO/response payloads this library targets,
and **they don't work today**: `ClassProperty` only recognizes `get`/`is`-prefixed
methods, but record accessors are `name()`, not `getName()`.
- [ ] Generalize property discovery: detect `Class::isRecord` and derive properties
      from `getRecordComponents()`; keep getter-based discovery for classic POJOs.
- [ ] Decide property-naming policy across both (record `name()` vs bean `Name` —
      paths are currently capitalized; unify, see Phase 2).

### 1b. `Map`, `Set`, array support
Currently only simple values, `List`, and structures are handled. Modern models use
all three constantly.
- [ ] `Map` matcher: compare by key, recurse into values, report missing/extra keys.
- [ ] `Set` / unordered collections: generalize the existing
      `IgnoreOrderListMatcher` logic.
- [ ] Array matcher (delegate to list logic after `Arrays.asList`-style adaptation).

### 1c. `Optional` handling
- [ ] Treat `Optional<T>` as nullable `T` (empty ≈ null) in the null-aware layer.

**Done when:** records, maps, sets, arrays, and `Optional` fields all match without
custom code.

---

## Phase 2 — API ergonomics (high impact, medium effort)

The stringly-typed, capitalized paths (`"Server.Ip"`) have no compile-time safety
and break silently on rename. This is a real adoption friction and a place to beat
the competition.

- [ ] Typed/lambda path API, e.g. `.with(matcher, BookSearchResult::getMetadata,
      SearchMetadata::getServer, Server::getIp)` — refactor-safe, IDE-completable.
- [ ] Keep string paths (incl. wildcards) as the dynamic/loosely-typed escape hatch.
- [ ] Reconsider the capitalized-path convention now that records are in play.

**Risk:** typed method-reference chains across nested generics get verbose; prototype
before committing. May land as an *additional* API rather than a replacement.

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
