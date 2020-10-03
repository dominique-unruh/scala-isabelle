package de.unruh.isabelle.experiments;

import de.unruh.isabelle.control.Isabelle;
import de.unruh.isabelle.control.IsabelleTest;
import de.unruh.isabelle.java.*;
import de.unruh.javapatterns.Capture;
import de.unruh.javapatterns.MatchManager;
import de.unruh.javapatterns.Pattern;
import de.unruh.javapatterns.PatternMatchReject;
import de.unruh.isabelle.mlvalue.MLFunction2;
import de.unruh.isabelle.mlvalue.MLValue;
import de.unruh.isabelle.pure.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import scala.Tuple2;

import java.nio.file.Path;

import static de.unruh.javapatterns.Pattern.capture;
import static de.unruh.javapatterns.Patterns.*;
import static de.unruh.javapatterns.Match.*;
import static de.unruh.isabelle.java.JPatterns.*;
import static de.unruh.isabelle.pure.Implicits.contextConverter;
import static de.unruh.isabelle.pure.Implicits.termConverter;
import static java.lang.System.out;
import static scala.concurrent.ExecutionContext.global;

public class JavaPatterns {
    public static <T,U> Pattern<Tuple2<T,U>> Pair(Pattern<? super T> pattern1, Pattern<? super U> pattern2) {
        return new Pattern<>() {
            @Override
            public void apply(@NotNull MatchManager mgr, @Nullable Tuple2<T, U> value) throws PatternMatchReject {
                if (value == null) reject();
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
                new Tuple2<>(new Tuple2<>(99, 2), new Tuple2<>(3, 4));

        Capture<Integer> x = capture("x"),
                         y = capture("y"),
                         z = capture("z"),
                         w = capture("w");

        Integer result = match(testValue,

                Pair(Pair(Is(99), y), Pair(z, w)),   () ->
                        y.v() + z.v() + w.v(),

                Pair(Pair(x, y), Pair(z, w)),   () ->
                        x.v() + y.v() + z.v() + w.v()

        );

        out.println(result);

        runExample(IsabelleTest.isabelleHome().toString());
    }


    // A function to replace occurrences of X+1 by X (for all X)
    static Term replace(Term term) throws Exception {
        Capture<Term> x = capture("x"),
                      t1 = capture("t1"),
                      t2 = capture("t2"),
                      body = capture("body");
        Capture<Typ> typ = capture("typ");
        Capture<String> name = capture("name");


        return match(term,

                App(App(Const(Is("Groups.plus_class.plus"), Any), x),
                        Const(Is("Groups.zero_class.zero"), Any)),
                () -> replace(x.v()),

                Abs(name, typ, body),
                () -> Abs.apply(name.v(), typ.v(), replace(body.v()),
                        isabelle, global()),

                App(t1, t2),
                () -> App.apply(replace(t1.v()), replace(t2.v()),
                        isabelle, global()),

                Any,
                () -> term);
    }


    private static Isabelle isabelle = null;

    static void runExample(String isabelleHome) throws Exception {
        // Initialize the Isabelle process with session HOL.
        Isabelle.Setup setup = JIsabelle.setup(Path.of(isabelleHome));
        // Differs from example in README: we skip building to make tests faster
        isabelle = new Isabelle(setup, false);

        // Load the Isabelle/HOL theory "Main" and create a context object
        Context ctxt = Context.apply("Main", isabelle, global());

        // Create a term by parsing a string
        Term term = Term.apply(ctxt, "x+0 = (y::nat)*1", isabelle, global());

        // Replace x+0 by x in the term above
        Term term2 = replace(term);

        // And pretty print the term
        out.println("term2: " + term2.pretty(ctxt, global()));
        // ==> term2: x = y * 1

        // Compile an ML function that can be executed directly in the Isabelle process
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






