package main.java.util;

import java.util.Optional;

/**
 * The class for computing the Levenshtein distance between two strings.
 *
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
