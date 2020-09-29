package de.unruh.isabelle.java.patterns;

import de.unruh.isabelle.java.patterns.Capture;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Predicate;

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

    public boolean excursion(MatchExcursion excursion) {
        return excursion(() -> { excursion.run(); return true; }, x -> true, false);
    }


    public <T> T excursion(MatchExcursionResult<T> excursion, Predicate<T> shouldReset, T failValue) {
        int size = captured.size();
        T result = failValue;
        try {
            result = excursion.run();
            if (shouldReset.test(result)) throw new PatternMatchReject();
        } catch (PatternMatchReject e) {
            while (captured.size() > size) {
                Capture<?> capture = captured.pop();
                capture.clear();
            }
        }
        return result;
    }

    public interface MatchExcursion {
        void run() throws PatternMatchReject;
    }

    public interface MatchExcursionResult<T> {
        T run() throws PatternMatchReject;
    }
}
