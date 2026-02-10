package org.dcsa.conformance.core.util;

import java.util.List;

public record Pair<T>(T first, T second) {

    public static <T> Pair<T> of(T first, T second) {
        return new Pair<>(first, second);
    }

    public List<T> values() {
        return List.of(first, second);
    }
}
