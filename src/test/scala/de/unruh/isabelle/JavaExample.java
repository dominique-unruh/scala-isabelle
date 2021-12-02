package de.unruh.isabelle;

import de.unruh.isabelle.control.Isabelle;
import de.unruh.isabelle.java.JIsabelle;
import de.unruh.isabelle.misc.Symbols;
import de.unruh.isabelle.mlvalue.MLFunction2;
import de.unruh.isabelle.mlvalue.MLValue;
import de.unruh.isabelle.pure.*;
import de.unruh.javapatterns.Capture;
import de.unruh.javapatterns.MatchException;

import java.nio.file.Path;

import static de.unruh.isabelle.java.JPatterns.*;
import static de.unruh.javapatterns.Match.match;
import static de.unruh.javapatterns.Pattern.capture;
import static de.unruh.javapatterns.Patterns.Any;
import static de.unruh.javapatterns.Patterns.Is;
import static java.lang.System.out;
import static de.unruh.isabelle.pure.Implicits.*;
import static scala.concurrent.ExecutionContext.global;

public class JavaExample {
    private Isabelle isabelle = null;

    /** A function to replace occurrences of X+1 by X (for all X).
     *
     * This version is written using only standard Java tools. See {@link #replace2} below for
     * a version written using pattern matching.
     * */
    @SuppressWarnings("InfiniteRecursion")
    Term replace(Term term) {
        term = term.concrete();

        // case App(App(Const("Groups.plus_class.plus", _), x), Const("Groups.zero_class.zero", _)) => replace(x)
        match1:
        {
            if (!(term instanceof App)) break match1;
            App app = (App) term;
            Term term1 = app.fun().concrete();
            Term term2 = app.arg().concrete();
            if (!(term1 instanceof App)) break match1;
            App app1 = (App) term1;
            Term term11 = app1.fun().concrete();
            Term x = app1.arg();
            if (!(term11 instanceof Const)) break match1;
            Const const11 = (Const) term11;
            if (!const11.name().equals("Groups.plus_class.plus")) break match1;
            if (!(term2 instanceof Const)) break match1;
            Const const2 = (Const) term2;
            if (!const2.name().equals("Groups.zero_class.zero")) break match1;

            return replace(x);
        }

        // case Abs(name, typ, body) => Abs(name, typ, replace(body))
        match2:
        {
            if (!(term instanceof Abs)) break match2;
            Abs abs = (Abs)term;
            String name = abs.name();
            Typ typ = abs.typ();
            Term body = abs.body();

            return Abs.apply(name, typ, replace(body), isabelle, global());
        }

        // case App(t1, t2) => App(replace(t1), replace(t2))
        match3:
        {
            if (!(term instanceof App)) break match3;
            App app = (App)term;
            Term t1 = app.fun();
            Term t2 = app.arg();

            return App.apply(replace(t1), replace(t2), isabelle, global());
        }

        // case _ => term
        return term;
    }

    /** A function to replace occurrences of X+1 by X (for all X).
     *
     * Written using pattern matching, does the same as {@link #replace}.
     */
    Term replace2(Term term) {
        Capture<Term> x = capture("x");
        Capture<Term> t1 = capture("t1");
        Capture<Term> t2 = capture("t2");
        Capture<String> name = capture("name");
        Capture<Typ> typ = capture("typ");
        Capture<Term> body = capture("body");

        try {
            return match(term,

                    App(App(Const(Is("Groups.plus_class.plus"), Any), x),
                            Const(Is("Groups.zero_class.zero"), Any)),
                    () -> replace2(x.v()),

                    Abs(name, typ, body),
                    () -> Abs.apply(name.v(), typ.v(), replace2(body.v()),
                            isabelle, global()),

                    App(t1, t2),
                    () -> App.apply(replace2(t1.v()), replace2(t2.v()),
                            isabelle, global()),

                    Any,
                    () -> term);
        } catch (MatchException e) {
            // Cannot happen
            throw new AssertionError("Unreachable code reached");
        }
    }


    public static void main(String[] args) {
        new JavaExample().runExample(args[0]);
    }

    void runExample(String isabelleHome) {
        // Initialize the Isabelle process with session HOL.
        // Differs from example in README: we skip building to make tests faster
        Isabelle.SetupGeneral setup = JIsabelle.setupSetBuild(false, JIsabelle.setup(Path.of(isabelleHome)));
        isabelle = new Isabelle(setup);

        // Load the Isabelle/HOL theory "Main" and create a context object
        Context ctxt = Context.apply("Main", isabelle, global());

        // Create a term by parsing a string
        Term term = Term.apply(ctxt, "x+0 = (y::nat)*1", Symbols.globalInstance(), isabelle, global());

        // Replace x+0 by x in the term above
        Term term2 = replace(term);

        // And pretty print the term
        out.println("term2: " + term2.pretty(ctxt, Symbols.globalInstance(), global()));
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

        out.println("term3: " + term3.pretty(ctxt, Symbols.globalInstance(), global()));
        // ==> term3: x = y

        // Destroy to save resources. (Not needed if the application ends here anyway.)
        isabelle.destroy();
    }
}
