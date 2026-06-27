# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A lightweight Java library for comparing two complex POJOs that lack (or cannot have) a meaningful `equals`. Comparison runs property-by-property via reflection and returns a `FeedbackNode` tree describing every mismatch, rather than a single boolean. Per-property rules ("matchers") let you loosen the comparison (range checks, regex, normalization, ignored fields, order-insensitive lists). The result tree can be serialized to JSON for storing/diffing large numbers of comparisons.

## Build & test

Use the committed **Gradle wrapper** (`./gradlew`, Gradle 8.10.2). Java toolchain is **17**. The build uses `java-library` + `maven-publish` with `api`/`implementation`/`testImplementation` configurations.

```bash
./gradlew build              # compile + test all three modules
./gradlew :core:test         # test a single module (core | json | examples)
./gradlew test --tests nl.alexeyu.structmatcher.matcher.ObjectMatcherTest          # single test class
./gradlew test --tests nl.alexeyu.structmatcher.matcher.ObjectMatcherTest.someMethod  # single test method
```

Tests run on the **JUnit 5 Platform**, but the test sources are still JUnit 4 (`org.junit.Test`, one `@RunWith(Theories.class)`, Mockito's `MockitoJUnitRunner`) executed via the **JUnit Vintage engine**. Per-test migration to Jupiter is deferred (see ROADMAP Phase 0/1). Mockito is used in one test; `json-path` and (in `examples`) Jackson XML/JSON load fixtures.

## Module layout

- **core** — the library. Zero runtime dependencies. All matching logic lives here.
- **json** — depends on `core` + Jackson. `Json.mapper()` returns a configured `ObjectMapper` that serializes a `FeedbackNode` tree to JSON (custom serializer for composite nodes; a mixin hides internal fields).
- **examples** — runnable end-to-end usage (the bookstore XML-vs-JSON scenario from the README). Model POJOs and integration tests live together under `examples/.../bookstore`.

## Architecture

### Entry point and the four-step flow
`ObjectMatcher.forClass(T.class)` → register custom matchers with `.with(matcher, "Dot.Separated.Path")` or `.withMatcher(matcher, "Path", "Parts")` → `.match(expected, actual)` → inspect the returned `FeedbackNode` (`feedback.isEmpty()` means they matched). See `matcher/ObjectMatcher.java`.

### Properties come from getters, by reflection
A "property" is a no-arg `getX()`/`isX()` method (`getClass` excluded). The property name is the getter name minus the `get`/`is` prefix, so paths are **capitalized**: `getServer().getIp()` → path `Server.Ip`. See `property/ClassProperty.java`. Only three shapes are supported: **simple** values (primitives, `Number`, `Boolean`, `Character`, `String`, enums), **`List`s**, and **structures** (everything else, recursed into). Arrays, sets, maps are **not** supported in a model.

### Default matcher selection
For each property, `Matchers.forProperty` picks a default: `valuesEqual()` for simple, `listsEqual()` for lists, `structuresEqual()` for structures (which recurses). Most matchers are wrapped `nullAware` (both-null matches; one-null is a mismatch).

### The matching stack and custom-matcher resolution (the key mechanism)
Matching is driven by a `ThreadLocal` `MatchingStack` (`MatchingStackHolder`), set up in `ObjectMatcher.match` and cleared in a `finally`. As the matcher tree descends into nested properties, `ContextAwareMatcher` pushes the current property name onto a `PropertyPath`. At each node, `DefaultMatchingStack.push` asks `WildcardMatcherResolver` whether a registered custom matcher's `PropertyPathPattern` matches the current path; if so it overrides the default, otherwise the default supplier is used. The stack also holds the top-level base/actual structures so indirect matchers can reach across the tree. **Implication:** matching is not thread-safe per `ObjectMatcher` call in the sense that it relies on thread-local state for the duration of one `match()` — keep matching synchronous within a thread.

### Path patterns support wildcards
A registered path may contain `*`, matching any run of intermediate properties — e.g. `*,Url` applies a URL matcher to every `Url` property anywhere, `A,*,C` matches `A,B,C`. Recursion logic and the full match/no-match table are in `matcher/WildcardPathChecker.java`.

### Indirect matchers are special-cased
A normal matcher receives `(propertyName, expectedValue, actualValue)` — values already extracted for that one property. An `IndirectMatcher` instead receives the **whole base and actual structures** and uses two fetcher functions to derive the values to compare. `ContextAwareMatcher` detects `instanceof IndirectMatcher` and feeds it the top-level structures from the stack rather than the local property values. Use this when the expected value for one field depends on a different part of the object. Created via `Matchers.indirectMatcher(...)`.

### Matcher composition
Matchers are a `@FunctionalInterface` (`FeedbackNode match(String, V, V)`). The `Matchers`, `StringMatchers`, and `IntegerMatchers` factories produce ~two dozen ready matchers. Compose them with `and(...)` (first non-empty feedback wins), the `normalizing` / `normalizingBase` / `normalizingBoth` wrappers (apply a `UnaryOperator` before delegating), `constant(v)`, `anyValue()`, `nonNull()`, `mustConform(predicate, spec)`, and the list matchers `listsEqual()` / `listsHaveEqualElements(comparator)`. To add a new built-in matcher, implement `Matcher<V>` (or return a lambda from a factory) and expose it through the relevant factory class.

### Feedback tree
Matchers return `FeedbackNode`s built by the `feedback/Feedback` factory: `ExpectationMet` (empty/leaf-OK), `ExpectationBroken` (leaf mismatch with expected/actual/spec), and `CompositeFeedbackNode` (a structure's children — empty only if all children empty). A `BrokenSpecificationException` is thrown when a *base* value violates a strict matcher's precondition (e.g. base is null under `nonNull()`), signaling a bad spec rather than a data mismatch.
