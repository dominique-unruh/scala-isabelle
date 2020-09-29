package de.unruh.isabelle.java.patterns;

// DOCUMENT?
public final class PatternMatchReject extends Exception {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
    PatternMatchReject() { super(); }
}
