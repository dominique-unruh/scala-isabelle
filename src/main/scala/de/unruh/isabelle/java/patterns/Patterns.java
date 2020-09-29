package de.unruh.isabelle.java.patterns;

import de.unruh.isabelle.pure.*;
import scala.MatchError;
import scala.Option;

import java.util.concurrent.Callable;

// DOCUMENT, mention (somewhere): can access captures already in match, can fail match in action
// TODO: Pattern for instance-match
// TODO: Pattern for predicate-match
// TODO: Pattern And (with varargs)
// TODO: Pattern Or (with varargs)
// TODO: Pattern Null, NotNull(_)
// TODO: NotMatch (negates the pattern, error if subpattern captures)
// TODO: Make a separate library of all this
// TODO: Can we handle exceptions better? (Avoid "throws Exception" clause)
public class Patterns {
    public static <T,X> Case<T,X> withCase(Pattern<? super T> pattern, Callable<? extends X> action) {
        return new Case<>(pattern, action);
    }

    @SafeVarargs
    public static <T,X> X match(T value, Case<T,X> ... cases) throws Exception {
        MatchManager mgr = new MatchManager();
        for (Case<T,X> cas : cases) {
            Option<X> result = cas.apply(mgr, value);
            if (result.nonEmpty())
                return result.get();
        }
        throw new MatchError(value);
    }


    public static <T> Pattern<T> Is(T expected) {
        return new Pattern<>() {
            @Override
            public void apply(MatchManager mgr, T value) throws PatternMatchReject {
                if (!expected.equals(value)) reject();
            }

            @Override
            public String toString() {
                return "="+expected;
            }
        };
    }

    public static final Pattern<Object> Any = new Pattern<>() {
        @Override
        public void apply(MatchManager mgr, Object value) {}

        @Override
        public String toString() {
            return "_";
        }
    };

}
