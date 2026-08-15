package nl.alexeyu.structmatcher.e2e;

import static java.util.Comparator.naturalOrder;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.inRange;
import static nl.alexeyu.structmatcher.matcher.IntegerMatchers.oneOf;
import static nl.alexeyu.structmatcher.matcher.Matchers.indirectMatcher;

import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;

import nl.alexeyu.structmatcher.e2e.DeploymentSnapshot.Metadata;
import nl.alexeyu.structmatcher.matcher.Matcher;
import nl.alexeyu.structmatcher.matcher.Matchers;
import nl.alexeyu.structmatcher.matcher.ObjectMatcher;
import nl.alexeyu.structmatcher.matcher.StringMatchers;

/**
 * Matching rules the end-to-end fixtures share. They tolerate the differences a second
 * environment introduces and hold the rest of the snapshot to equality.
 */
final class DeploymentSnapshotSpec {

    private static final String REQUEST_ID = "req-(json|xml)-[0-9]{3}";

    private static final String TIMESTAMP = "2026-08-15T[0-9]{2}:[0-9]{2}:[0-9]{2}Z";

    private static final String HTTPS_URL = "https://[a-z0-9.-]+(/[a-z0-9./-]*)?";

    private DeploymentSnapshotSpec() {
    }

    static ObjectMatcher<DeploymentSnapshot> matcher() {
        return ObjectMatcher.forClass(DeploymentSnapshot.class)
                .with(StringMatchers.regex(REQUEST_ID), DeploymentSnapshot::getMetadata,
                        Metadata::requestId)
                .with(StringMatchers.regex(TIMESTAMP), DeploymentSnapshot::getMetadata,
                        Metadata::generatedAt)
                // A service Url may move between environments. The control-plane one may not,
                // so the exact registration below overrides the wildcard on that path.
                .with(StringMatchers.regex(HTTPS_URL), "*.Url")
                .with(Matchers.<String>valuesEqual(), DeploymentSnapshot::getMetadata,
                        Metadata::url)
                .with(oneOf(8080, 8081, 8443), "Services.Endpoint.Port")
                .with(inRange(0, 2000), "Services.Instances.LatencyMs")
                .with(unorderedCaseInsensitiveStrings(), "Services.Tags")
                .with(normalizedNonEmptyString(), "Approval.Reviewer")
                .with(reportedInstanceCount(), DeploymentSnapshot::getMetadata,
                        Metadata::reportedInstances);
    }

    private static Matcher<List<String>> unorderedCaseInsensitiveStrings() {
        UnaryOperator<List<String>> lowerCase = values -> values.stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        return Matchers.<String>listsHaveEqualElements(naturalOrder())
                .normalizingBoth(lowerCase);
    }

    private static Matcher<String> normalizedNonEmptyString() {
        UnaryOperator<String> normalize = value -> value.trim().toLowerCase(Locale.ROOT);
        return Matchers.<String>nonNull().and(StringMatchers.nonEmpty())
                .and(Matchers.<String>valuesEqual().normalizingBoth(normalize));
    }

    private static Matcher<DeploymentSnapshot> reportedInstanceCount() {
        return indirectMatcher("ReportedInstances", Matchers.<Integer>valuesEqual(),
                snapshot -> snapshot.getMetadata().reportedInstances(),
                snapshot -> snapshot.getServices().stream()
                        .mapToInt(service -> service.getInstances().size()).sum());
    }

}
