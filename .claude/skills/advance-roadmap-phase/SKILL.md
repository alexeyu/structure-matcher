---
name: advance-roadmap-phase
description: Implement the next item from ROADMAP.md in this structure-matcher
  repo, following its small-verified-steps workflow and conventions.
---

# Advancing a ROADMAP phase

Read ROADMAP.md and pick (or confirm) the target item. Work in small,
individually-verified steps — never a big-bang change.

## Invariants (do not violate)
- `core` stays **zero runtime dependencies**. A concern that *consumes* the
  FeedbackNode tree (serialize, aggregate, render, query) goes in a **sibling
  module** depending on `core` (precedent: `json`, `report`) — not in core.
- Paths produced anywhere must match registration-path style: structure
  children dot-joined, collection elements `parent[index]` (see FeedbackPaths).

## Per step
1. Implement the smallest shippable slice.
2. Test it: `./gradlew :<module>:test --tests '<FQCN>'`, then `./gradlew build`
   for the whole tree (build runs spotlessCheck).
3. `./gradlew spotlessApply` before finishing.

## Tests
- JUnit 4 sources run via the JUnit 5 Vintage engine; mirror existing style
  (org.junit.Test, assertEquals/assertTrue).
- Unit-test tree logic with hand-built `Feedback.*` trees; add **one** live
  `ObjectMatcher.forClass(...).match(...)` end-to-end test to pin real naming.
- A module's tests can't see another module's test fixtures — add a small local
  model instead of reaching into core's test sources.

## When the slice lands
- Tick the ROADMAP box and add an honest note: what shipped, deviations from the
  original plan, and remaining limitations/follow-ups.
- Update CLAUDE.md if architecture/module layout changed.
- **Positioning honesty:** don't claim differentiation the library doesn't have
  (a structured POJO diff alone is commodity — JaVers/java-object-diff). The real
  niche is tolerant + cross-field matching with a corpus report. See ROADMAP
  "Positioning".
