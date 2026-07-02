package nl.alexeyu.structmatcher.assertj;

/**
 * Entry point for using structure-matcher inside an <a href="https://assertj.github.io">AssertJ</a>
 * test suite. Statically import {@link #assertThat} and assert that an object is
 * <em>equivalent enough</em> to an expected one under per-field rules:
 *
 * <pre>
 * import static nl.alexeyu.structmatcher.assertj.StructMatcherAssertions.assertThat;
 *
 * assertThat(actualResponse).matchesStructure(expectedResponse);
 *
 * // ...or with a configured spec (tolerant / cross-field rules):
 * var spec = ObjectMatcher.forClass(Response.class)
 *         .with(StringMatchers.url(), "*", "Url");
 * assertThat(actualResponse).matchesStructure(expectedResponse, spec);
 * </pre>
 *
 * On a mismatch the test fails with the structured, per-field diff (every broken path with its
 * expected/actual detail) rather than a single boolean — so the failure localizes exactly which
 * fields diverged.
 */
public final class StructMatcherAssertions {

    private StructMatcherAssertions() {
    }

    /** Starts an assertion on {@code actual}. @see StructureAssert */
    public static <T> StructureAssert<T> assertThat(T actual) {
        return new StructureAssert<>(actual);
    }

}
