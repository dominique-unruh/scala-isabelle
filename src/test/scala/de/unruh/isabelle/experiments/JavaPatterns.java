package de.unruh.isabelle.experiments;

import de.unruh.isabelle.control.Isabelle;
import de.unruh.isabelle.control.IsabelleTest;
import de.unruh.isabelle.java.JIsabelle;
import de.unruh.isabelle.mlvalue.MLFunction2;
import de.unruh.isabelle.mlvalue.MLValue;
import de.unruh.isabelle.pure.*;
import scala.MatchError;
import scala.Option;
import scala.Some;
import scala.Tuple2;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Callable;

import static de.unruh.isabelle.pure.Implicits.contextConverter;
import static de.unruh.isabelle.pure.Implicits.termConverter;
import static java.lang.System.out;
import static scala.concurrent.ExecutionContext.global;

final class PatternMatchReject extends Exception {
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}

abstract class Pattern<T> {
    protected abstract void apply(MatchManager mgr, T value) throws PatternMatchReject;
    public final <U extends T> Pattern<U> contravariance() {
        //noinspection unchecked
        return (Pattern<U>)this;
    }

    @Override
    public abstract String toString();
}

final class Capture<T> extends Pattern<T> {
    private final String name;

    @Override
    public String toString() { return name; }

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
            throw new RuntimeException("Reading undefined capture variable "+name); // TODO: specific exception type
        return value;
    }

    @Override
    protected void apply(MatchManager mgr, T value) {
//        out.println("Assigning "+name+" "+value+" "+assigned);
        if (assigned)
            throw new RuntimeException("Re-assigned "+name+" in pattern match"); // TODO: specific exception type
        mgr.assigned(this);
        assigned = true;
        this.value = value;
    }
}

abstract class Case<In,Return> {
    protected abstract Option<Return> apply(MatchManager msg, In in) throws Exception;
}

final class MatchManager {
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

public class JavaPatterns {

    public static <T> Pattern<T> Eq(T expected) {
        return new Pattern<>() {
            @Override
            protected void apply(MatchManager mgr, T value) throws PatternMatchReject {
                if (!expected.equals(value))
                    throw new PatternMatchReject();
            }

            @Override
            public String toString() {
                return "="+expected;
            }
        };
    }


    public static final Pattern<Object> any = new Pattern<>() {
        @Override
        protected void apply(MatchManager mgr, Object value) {
        }

        @Override
        public String toString() {
            return "_";
        }
    };
    public static <T> Pattern<T> Any() {
        return any.contravariance();
    }

    public static <T,X> Case<T,X> Case(Pattern<? super T> pattern, Callable<? extends X> action) {
      return new Case<>() {
          @Override
          protected Option<X> apply(MatchManager mgr, T t) throws Exception {
//              out.println("Starting case");
              try {
                  pattern.apply(mgr, t);
//                  out.println("Running case action");
//                  out.println("Case "+pattern+" match: "+t);
                  return new Some<>(action.call());
              } catch (PatternMatchReject e) {
//                  out.println("Case "+pattern+" fail: "+t);
                  return Option.empty();
              } finally {
                  mgr.clearCaptured();
              }
          }
      };
    }

    @SafeVarargs
    public static <T,X> X Match(T value, Case<T,X> ... cases) throws Exception {
        MatchManager mgr = new MatchManager();
        for (Case<T,X> cas : cases) {
            Option<X> result = cas.apply(mgr, value);
            if (result.nonEmpty())
                return result.get();
        }
        throw new MatchError(value);
    }

    public static <T,U> Pattern<Tuple2<T,U>> Pair(Pattern<? super T> pattern1, Pattern<? super U> pattern2) {
        return new Pattern<>() {
            @Override
            protected void apply(MatchManager mgr, Tuple2<T, U> value) throws PatternMatchReject {
                pattern1.apply(mgr, value._1());
                pattern2.apply(mgr, value._2());
            }

            @Override
            public String toString() {
                return "("+pattern1.toString()+","+pattern2.toString()+")";
            }
        };
    }

    public static void main(String[] args) throws Exception {
        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> testValue =
                new Tuple2<>(new Tuple2<>(1, 2), new Tuple2<>(3, 4));
/*
        Capture<Integer> x = new Capture<>("x");
        Capture<Integer> y = new Capture<>("y");
        Capture<Integer> z = new Capture<>("z");
        Capture<Integer> w = new Capture<>("w");

        Integer result = Match(testValue,

                Case(
                        Pair(Pair(Eq(99), y), Pair(z, w)),   () ->
                        y.v() + z.v() + w.v()),

                Case(
                        Pair(Pair(x, y), Pair(z, w)),   () ->
                        x.v() + y.v() + z.v() + w.v())

        );

        out.println(result);*/

        runExample(IsabelleTest.isabelleHome().toString());
    }

    public static Pattern<Term> Const(Pattern<? super String> p1, Pattern<? super Typ> p2) {
        return new Pattern<>() {
            @Override
            protected void apply(MatchManager mgr, Term term) throws PatternMatchReject {
                term = term.concrete();
//                out.println("Const: "+term);
                if (!(term instanceof Const)) throw new PatternMatchReject();
                Const con = (Const)term;
                p1.apply(mgr, con.name());
                p2.apply(mgr, con.typ());

            }

            @Override
            public String toString() {
                return "Const("+p1+","+p2+")";
            }
        };
    }
    public static Pattern<Term> App(Pattern<? super Term> p1, Pattern<? super Term> p2) {
        return new Pattern<Term>() {
            @Override
            protected void apply(MatchManager mgr, Term term) throws PatternMatchReject {
                term = term.concrete();
//                out.println("App: "+term);
                if (!(term instanceof App)) throw new PatternMatchReject();
                App app = (App)term;
                p1.apply(mgr, app.fun());
                p2.apply(mgr, app.arg());
            }

            @Override
            public String toString() {
                return "App("+p1+","+p2+")";
            }
        };
    }

    public static Pattern<Term> Abs(Pattern<? super String> name, Pattern<? super Typ> typ, Pattern<? super Term> body) {
        return new Pattern<Term>() {
            @Override
            protected void apply(MatchManager mgr, Term term) throws PatternMatchReject {
                term = term.concrete();
                if (!(term instanceof Abs)) throw new PatternMatchReject();
                Abs abs = (Abs)term;
                name.apply(mgr, abs.name());
                typ.apply(mgr, abs.typ());
                body.apply(mgr, abs.body());
            }

            @Override
            public String toString() {
                return "Abs("+name+","+typ+","+body+")";
            }
        };
    }

    // A function to replace occurrences of X+1 by X (for all X)
    static Term replace(Term term) throws Exception {
        Capture<Term> x = new Capture<>("x");
        Capture<Term> t1 = new Capture<>("t1");
        Capture<Term> t2 = new Capture<>("t2");
        Capture<String> name = new Capture<>("name");
        Capture<Typ> typ = new Capture<>("typ");
        Capture<Term> body = new Capture<>("body");


        return Match(term,

                Case(
                        App(App(Const(Eq("Groups.plus_class.plus"), any), x),
                                Const(Eq("Groups.zero_class.zero"), any)),
                        () -> replace(x.v())),

                Case(
                        Abs(name, typ, body),
                        () -> Abs.apply(name.v(), typ.v(), replace(body.v()),
                                isabelle, global())),

                Case(
                        App(t1, t2),
                        () -> App.apply(replace(t1.v()), replace(t2.v()),
                                isabelle, global())),

                Case(
                        any,
                        () -> term));
    }


    private static Isabelle isabelle = null;

    static void runExample(String isabelleHome) throws Exception {
        // Initialize the Isabelle process with session HOL.
        Isabelle.Setup setup = JIsabelle.setup(Path.of(isabelleHome));
        // Differs from example in README: we skip building to make tests faster
        isabelle = new Isabelle(setup, false);

        // Load the Isabelle/HOL theory "Main" and create a context objglobal()t
        Context ctxt = Context.apply("Main", isabelle, global());

        // Create a term by parsing a string
        Term term = Term.apply(ctxt, "x+0 = (y::nat)*1", isabelle, global());

        // Replace x+0 by x in the term above
        Term term2 = replace(term);

        // And pretty print the term
        out.println("term2: " + term2.pretty(ctxt, global()));
        // ==> term2: x = y * 1

        // Compile an ML function that can be exglobal()uted dirglobal()tly in the Isabelle process
        MLFunction2<Context, Term, Term> simplify =
                MLValue.compileFunction("fn (ctxt,t) => Thm.cterm_of ctxt t |> Simplifier.asm_full_rewrite ctxt " +
                                "|> Thm.rhs_of |> Thm.term_of",
                        isabelle, global(),
                        contextConverter(), termConverter(), termConverter());

        // Simplify term2
        Term term3 = simplify
                .apply(ctxt, term2, isabelle, global(), contextConverter(), termConverter())
                .retrieveNow(termConverter(), isabelle, global());

        out.println("term3: " + term3.pretty(ctxt, global()));
        // ==> term3: x = y
    }
}






