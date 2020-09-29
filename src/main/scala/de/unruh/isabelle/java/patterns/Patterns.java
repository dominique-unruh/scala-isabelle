package de.unruh.isabelle.java.patterns;

import de.unruh.isabelle.pure.*;
import scala.MatchError;
import scala.Option;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// DOCUMENT, mention (somewhere): can access captures already in match, can fail match in action
// TODO: Make a separate library of all this
// TODO: Can we handle exceptions better? (Avoid "throws Exception" clause)
// TODO: Test cases
public class Patterns {
    public static <T, X> Case<T, X> withCase(Pattern<? super T> pattern, Callable<? extends X> action) {
        return new Case<>(pattern, action);
    }

    @SafeVarargs
    public static <T, X> X match(T value, Case<T, X>... cases) throws Exception {
        MatchManager mgr = new MatchManager();
        for (Case<T, X> cas : cases) {
            Option<X> result = cas.apply(mgr, value);
            if (result.nonEmpty())
                return result.get();
        }
        throw new MatchError(value);
    }

    public static <T, X> X match(T value, Pattern<? super T> pattern1, Callable<? extends X> action1) throws Exception {
        return match(value, withCase(pattern1, action1));
    }

    public static <T, X> X match(T value, Pattern<? super T> pattern1, Callable<? extends X> action1,
                                 Pattern<? super T> pattern2, Callable<? extends X> action2) throws Exception {
        return match(value, withCase(pattern1, action1), withCase(pattern2, action2));
    }

    public static <T, X> X match(T value, Pattern<? super T> pattern1, Callable<? extends X> action1,
                                 Pattern<? super T> pattern2, Callable<? extends X> action2,
                                 Pattern<? super T> pattern3, Callable<? extends X> action3) throws Exception {
        return match(value, withCase(pattern1, action1), withCase(pattern2, action2), withCase(pattern3, action3));
    }

    public static <T, X> X match(T value, Pattern<? super T> pattern1, Callable<? extends X> action1,
                                 Pattern<? super T> pattern2, Callable<? extends X> action2,
                                 Pattern<? super T> pattern3, Callable<? extends X> action3,
                                 Pattern<? super T> pattern4, Callable<? extends X> action4) throws Exception {
        return match(value, withCase(pattern1, action1), withCase(pattern2, action2), withCase(pattern3, action3),
                withCase(pattern4, action4));
    }
    // TODO: more of those

    public static <T> Pattern<T> Is(T expected) {
        return new Pattern<>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                if (!expected.equals(value)) reject();
            }

            @Override
            public String toString() {
                return "=" + expected;
            }
        };
    }

    public static final Pattern<Object> Any = new Pattern<>() {
        @Override
        public void apply(MatchManager mgr, Object value) {
        }

        @Override
        public String toString() {
            return "_";
        }
    };

    public static final Pattern<Object> Null = new Pattern<>() {
        @Override
        public void apply(MatchManager mgr, Object value) throws PatternMatchReject {
            if (value != null) reject();
        }

        @Override
        public String toString() {
            return "null";
        }
    };

    public static <T> Pattern<T> NotNull(Pattern<? super T> pattern) {
        return new Pattern<>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                if (value == null) reject();
                pattern.apply(mgr, value);
            }

            @Override
            public String toString() {
                return "null";
            }
        };
    }

    @SafeVarargs
    public static <T> Pattern<T> And(Pattern<? super T>... patterns) {
        return new Pattern<>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                for (Pattern<? super T> pattern : patterns)
                    pattern.apply(mgr, value);
            }

            @Override
            public String toString() {
                StringJoiner joiner = new StringJoiner(", ");
                for (Pattern<?> pattern : patterns)
                    joiner.add(pattern.toString());
                return "And(" + joiner + ")";
            }
        };
    }

    @SafeVarargs
    public static <T> Pattern<T> Or(Pattern<? super T>... patterns) {
        return new Pattern<T>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                if (patterns.length == 0) reject();
                for (int i=0; i<patterns.length-1; i++) {
                    Pattern<? super T> pattern = patterns[i];
                    if (mgr.excursion(() -> pattern.apply(mgr, value))) return;
                }
                patterns[patterns.length-1].apply(mgr, value);
            }

            @Override
            public String toString() {
                StringJoiner joiner = new StringJoiner(", ");
                for (Pattern<?> pattern : patterns)
                    joiner.add(pattern.toString());
                return "Or(" + joiner + ")";
            }
        };
    }

    public static <T,U> Pattern<T> Instance(Class<U> clazz, Pattern<? super U> pattern) {
        return new Pattern<T>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                U castValue = null;
                try {
                    castValue = clazz.cast(value);
                } catch (ClassCastException e) {
                    reject();
                }
                pattern.apply(mgr,castValue);
            }

            @Override
            public String toString() {
                return "Instance("+clazz.getSimpleName()+","+pattern+")";
            }
        };
    }

    public static <T> Pattern<T> Pred(Predicate<? super T> predicate) {
        return new Pattern<T>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                if (!predicate.test(value)) reject();
            }

            @Override
            public String toString() {
                return "Pred(...)";
            }
        };
    }

    public static <T> Pattern<T> NoMatch(Pattern<? super T> pattern) {
        return new Pattern<T>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                boolean matched = mgr.excursion(() -> pattern.apply(mgr, value));
                if (matched) reject();
            }

            @Override
            public String toString() {
                return "NoMatch("+pattern+")";
            }
        };
    }
}