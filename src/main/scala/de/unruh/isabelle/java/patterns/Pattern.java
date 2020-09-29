package de.unruh.isabelle.java.patterns;

// DOCUMENT
public abstract class Pattern<T> {
    public abstract void apply(MatchManager mgr, T value) throws PatternMatchReject;

    public final <U extends T> Pattern<U> contravariance() {
        //noinspection unchecked
        return (Pattern<U>) this;
    }

    @Override
    public abstract String toString();

    public static void reject() throws PatternMatchReject {
        throw new PatternMatchReject();
    }
}
