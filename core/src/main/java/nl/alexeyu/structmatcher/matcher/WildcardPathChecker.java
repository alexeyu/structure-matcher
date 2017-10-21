package nl.alexeyu.structmatcher.matcher;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Check a path to a registered custom matcher against a stack of nested
 * properties.' The path may contain wildcards (*), which mean, any subset of
 * properties between two concrete path elements would match. Wildcards are
 * convenient when there are sub-properties with the same name and type in the
 * different objects of the model. For instance, all properties with the
 * name/type 'url' can be validated with the same URL matcher.
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
class WildcardPathChecker implements BiPredicate<List<String>, List<String>> {

    @Override
    public boolean test(List<String> pattern, List<String> path) {
        if (path.isEmpty() && isPositive(pattern)) {
            return true;
        }
        if (path.isEmpty() || pattern.isEmpty()) {
            return false;
        }
        String nextPattern = pattern.get(0);
        String nextPath = path.get(0);
        if (nextPattern.equals(nextPath)) {
            return test(tail(pattern), tail(path));
        }
        if (isWildcard(nextPattern)) {
            if (pattern.size() > 1) {
                if (pattern.get(1).equals(nextPath) || isWildcard(pattern.get(1))) {
                    return test(tail(pattern), path);
                }
            }
            return test(pattern, tail(path));
        }
        return false;
    }

    private boolean isPositive(List<String> pattern) {
        return pattern.stream().allMatch(this::isWildcard);
    }

    private boolean isWildcard(String s) {
        return "*".equals(s);
    }

    private List<String> tail(List<String> list) {
        return list.subList(1, list.size());
    }
}
