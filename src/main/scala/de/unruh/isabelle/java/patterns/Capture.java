package de.unruh.isabelle.java.patterns;

// DOCUMENT
final public class Capture<T> extends Pattern<T> {
    private final String name;

    @Override
    public String toString() {
        return name;
    }

    public Capture(String name) {
        this.name = name;
    }

    private T value;
    private boolean assigned = false;

    void clear() {
//        out.println("Resetting "+name+" "+value+" "+assigned);
        assigned = false;
    }

    public T v() {
//        out.println("Reading "+name+" "+value+" "+assigned);
        if (!assigned)
            throw new RuntimeException("Reading undefined capture variable " + name); // TODO: specific exception type
        return value;
    }

    @Override
    public void apply(MatchManager mgr, T value) {
//        out.println("Assigning "+name+" "+value+" "+assigned);
        if (assigned)
            throw new RuntimeException("Re-assigned " + name + " in pattern match"); // TODO: specific exception type
        mgr.assigned(this);
        assigned = true;
        this.value = value;
    }
}
