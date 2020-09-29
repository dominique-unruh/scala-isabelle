package de.unruh.isabelle.java.patterns;

import de.unruh.isabelle.java.patterns.Capture;

import java.util.ArrayDeque;
import java.util.Deque;

// DOCUMENT
public final class MatchManager {
    // Making this package private
    MatchManager() {}

    // TODO add support for atomically trying a block of code (in PatternMatchReject is thrown, should revert all assignments
    Deque<Capture<?>> captured = new ArrayDeque<>(10);

    <T> void assigned(Capture<T> x) {
        captured.add(x);
    }

    void clearCaptured() {
        for (Capture<?> capture : captured)
            capture.clear();
        captured.clear();
    }
}
