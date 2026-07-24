package nl.alexeyu.structmatcher.examples.bookstore;

import static nl.alexeyu.structmatcher.assertj.StructMatcherAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * Shows the {@code assertj} bridge in the bookstore scenario: assert straight from an existing
 * AssertJ test that a response is <em>equivalent enough</em> to a baseline under the shared
 * {@link ContextTolerantSpec}. The prod and mobile responses both pass - one differs only in
 * execution context, the other only in how it presents the same answer - while the regressed
 * response fails, and the AssertJ message names the two fields that actually changed rather than
 * anything the spec tolerates. Same fixtures as {@link ResponseMatchingTest}.
 */
public class AssertJExampleTest {

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest, mobileRegression;

    @BeforeEach
    public void setUp() throws Exception {
        rootPath = Paths.get(AssertJExampleTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        var jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
        mobileRegression = fromFile(jsonMapper, "response-on-smoke-for-mobile-regression.json");
        desktopProd = fromFile(new XmlMapper(), "response-on-smoke-for-desktop-prod.xml");
    }

    private BookSearchResult fromFile(ObjectMapper mapper, String fileName) throws Exception {
        return mapper.readValue(rootPath.resolve(fileName).toFile(), BookSearchResult.class);
    }

    @Test
    public void prodIsEquivalentToTestUnderTolerantMetadata() {
        assertThat(desktopProd).matchesStructure(desktopTest, ContextTolerantSpec.matcher());
    }

    @Test
    public void mobileIsEquivalentDespiteAbbreviatedPresentation() {
        // The initials and the omitted publishing details are presentation, not a different
        // answer, so the assertion passes.
        assertThat(mobileTest).matchesStructure(desktopTest, ContextTolerantSpec.matcher());
    }

    @Test
    public void aGenuineRegressionFailsAndTheMessageNamesTheField() {
        // Same tolerated presentation, but a changed title and a hit count that disagrees with
        // the returned books - the failure points at exactly those two fields.
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> assertThat(mobileRegression)
                        .matchesStructure(desktopTest, ContextTolerantSpec.matcher()))
                .withMessageContaining("[Books[0].Title]")
                .withMessageContaining("[Metadata.BooksFound]")
                // Nothing tolerated rides along: no server, timing or author-name diff line.
                .matches(e -> !e.getMessage().contains("[Metadata.Server"),
                        "no tolerated metadata field appears in the diff")
                .matches(e -> !e.getMessage().contains("FirstName"),
                        "the abbreviated first name is tolerated, not reported");
    }

}
