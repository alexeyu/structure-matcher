package nl.alexeyu.structmatcher.matcher;

import java.util.function.BiPredicate;

import nl.alexeyu.structmatcher.property.PropertyPath;
import nl.alexeyu.structmatcher.property.PropertyPathPattern;

/**
 * Checks the path a custom matcher was registered for against the stack of nested properties the
 * traversal has reached. A path may contain wildcards (*), each standing for any run of properties
 * between two concrete segments. Reach for one when the same name and type recurs across the model:
 * the path <code>*,Url</code> puts one URL matcher on every 'url' property, wherever it sits.
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
        // A wildcard absorbs zero or more leading segments, so try both branches: end it here, or
        // let it swallow one more segment. Both are needed when a literal after the wildcard also
        // occurs among the segments it absorbs: `*,A` matches `A,A` only if `*` takes the first.
        return test(pattern.tail(), path) || test(pattern, path.tail());
    }

}
