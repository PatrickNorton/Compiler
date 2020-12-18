package main.java.util;

import java.util.Optional;

/**
 * The class for computing the Levenshtein distance between two strings.
 *
 * <p>
 *     The Levenshtein distance is the minimum number of single-character edits
 *     required to transform a string into another. A single-character edit is
 *     defined as an insertion, deletion, or substitution.
 * </p>
 * @author Patrick Norton
 */
public final class Levenshtein {
    private Levenshtein() {}

    /**
     * Computes the Levenshtein distance between the two given strings.
     *
     * @param a The first string
     * @param b The second string
     * @return The distance between the two
     */
    public static int distance(String a, String b) {
        if (a.isEmpty()) {
            return b.length();
        } else if (b.isEmpty()) {
            return a.length();
        }

        var dCol = numbers(b.length());
        int tLast = 0;

        for (int i = 0; i < a.length(); i++) {
            var sc = a.charAt(i);
            int current = i;
            dCol[0] = current + 1;

            for (int j = 0; j < b.length(); j++) {
                var tc = b.charAt(j);
                var next = dCol[j + 1];
                if (sc == tc) {
                    dCol[j + 1] = current;
                } else {
                    dCol[j + 1] = Math.min(current, next);
                    dCol[j + 1] = Math.min(dCol[j + 1], dCol[j]) + 1;
                }
                current = next;
                tLast = j;
            }
        }
        return dCol[tLast + 1];
    }

    /**
     * Finds the closest match to the given value from an {@link Iterable} of
     * candidates.
     *
     * <p>
     *     This institutes a threshold for returning a match: It will only
     *     return if the Levenshtein distance is more than one-third of the
     *     length of the original {@link String}. This is to prevent reporting
     *     values that are unlikely to be a typo.
     * </p>
     * @param name The name to compare values to
     * @param candidates The candidates to search
     * @return The closest candidate, if within the threshold
     */
    public static Optional<String> closestName(String name, Iterable<String> candidates) {
        String min = null;
        int dist = Integer.MAX_VALUE;
        for (var variable : candidates) {
            var levDist = Levenshtein.distance(name, variable);
            if (levDist < dist || min == null) {
                min = variable;
                dist = levDist;
            }
        }
        var maxDistance = Math.max(name.length(), 3) / 3;
        return dist > maxDistance ? Optional.empty() : Optional.ofNullable(min);
    }

    private static int[] numbers(int length) {
        int[] result = new int[length + 1];
        for (int i = 0; i < length + 1; i++) {
            result[i] = i;
        }
        return result;
    }
}
