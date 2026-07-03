package nl.alexeyu.structmatcher.matcher;

import java.util.function.BiPredicate;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

/**
 * Check a path to a registered custom matcher against a stack of nested properties. The path may
 * contain wildcards (*), which mean, any subset of properties between two concrete path elements
 * would match. Wildcards are convenient when there are sub-properties with the same name and type
 * in the different objects of the model. For instance, all properties with the name/type 'url' can
 * be validated with the same URL matcher.
 * <p>
 * Examples:
 * <table>
 * <th>Path</th>
 * <th>Property stack</th>
 * <th>Result</th>
 * <tr>
 * <td>A,B,C</td>
 * <td>A,B,C</td>
 * <td>Match</td>
 * </tr>
 * <tr>
 * <td>A,*</td>
 * <td>A,B,C</td>
 * <td>Match</td>
 * </tr>
 * <tr>
 * <td>A,*,C</td>
 * <td>A,B,C</td>
 * <td>Match</td>
 * </tr>
 * <tr>
 * <td>*,C</td>
 * <td>A,B,C</td>
 * <td>Match</td>
 * </tr>
 * <tr>
 * <td>*,B</td>
 * <td>A,B,C</td>
 * <td>Do not match - property stack is longer</td>
 * </tr>
 * <tr>
 * <td>A,B,C</td>
 * <td>A,B</td>
 * <td>Do not match - there is one more expectation in the path.</td>
 * </tr>
 * </table>
 */
final class WildcardPathChecker implements BiPredicate<PropertyPathPattern, PropertyPath> {

    @Override
    public boolean test(PropertyPathPattern pattern, PropertyPath path) {
        if (path.isEmpty() && pattern.isPositive()) {
            return true;
        }
        if (path.isEmpty() || pattern.isEmpty()) {
            return false;
        }
        if (pattern.headsMatch(path)) {
            return test(pattern.tail(), path.tail());
        }
        if (!pattern.startsWithWildcard()) {
            return false;
        }
        // A wildcard can absorb zero-or-more leading path segments. Try both: let it end here
        // (advance past the wildcard, keep the path) or absorb one more segment (keep the
        // wildcard, drop a path head). Both branches are needed to backtrack correctly when a
        // literal after the wildcard also occurs among the segments it should absorb — e.g.
        // pattern `*,A` against path `A,A` must let `*` swallow the first `A`.
        return test(pattern.tail(), path) || test(pattern, path.tail());
    }

}
