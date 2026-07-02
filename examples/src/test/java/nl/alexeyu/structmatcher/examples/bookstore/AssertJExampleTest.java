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
 * {@link ContextTolerantSpec} (metadata tolerated, book payload strict). The prod response matches
 * because only its execution context differs; the mobile response fails because its book payload
 * genuinely changed — and the AssertJ failure localizes that to {@code Books[...]}, not the
 * tolerated metadata. Same fixtures as {@link ResponseMatchingTest}.
 */
public class AssertJExampleTest {

    private Path rootPath;

    private BookSearchResult desktopTest, desktopProd, mobileTest;

    @BeforeEach
    public void setUp() throws Exception {
        rootPath = Paths.get(AssertJExampleTest.class.getResource("/").toURI())
                .resolve("../../../resources/test");
        var jsonMapper = new ObjectMapper();
        desktopTest = fromFile(jsonMapper, "response-on-smoke-for-desktop-test.json");
        mobileTest = fromFile(jsonMapper, "response-on-smoke-for-mobile-test.json");
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
    public void mobileFailsOnTheBookPayloadDespiteTolerantMetadata() {
        // The metadata rules tolerate the different server/platform, but the abbreviated author
        // names and dropped meta are a real payload change — the failure points at the books.
        assertThatExceptionOfType(AssertionError.class)
                .isThrownBy(() -> assertThat(mobileTest)
                        .matchesStructure(desktopTest, ContextTolerantSpec.matcher()))
                .withMessageContaining("Books[0].Authors[0].FirstName")
                .withMessageContaining("Books[0].Meta")
                // No tolerated metadata field appears as a diff line (the paths are bracketed).
                .matches(e -> !e.getMessage().contains("[Metadata"),
                        "no tolerated metadata field appears in the diff");
    }

}
