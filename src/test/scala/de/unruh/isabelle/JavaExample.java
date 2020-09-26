package de.unruh.isabelle;

import de.unruh.isabelle.control.Isabelle;
import de.unruh.isabelle.java.JIsabelle;
import de.unruh.isabelle.mlvalue.MLFunction2;
import de.unruh.isabelle.mlvalue.MLValue;
import de.unruh.isabelle.pure.*;
import scala.concurrent.ExecutionContext;

import java.nio.file.Path;

import static java.lang.System.out;
import static de.unruh.isabelle.pure.Implicits.*;
import static de.unruh.isabelle.mlvalue.Implicits.*;
import static scala.concurrent.ExecutionContext.global;

public class JavaExample {
    private Isabelle isabelle = null;

    // A function to replace occurrences of X+1 by X (for all X)
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

    public static void main(String[] args) {
        new JavaExample().runExample(args[0]);
    }

    void runExample(String isabelleHome) {
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
