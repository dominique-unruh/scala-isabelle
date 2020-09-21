# scala-isabelle

[![Build Status](https://travis-ci.com/dominique-unruh/scala-isabelle.svg?branch=master)](https://travis-ci.com/dominique-unruh/scala-isabelle)

TODO: Add more explanations

Example:
```Scala
// Initialize the Isabelle process with session HOL.
// We assume an Isabelle installation in /opt/Isabelle2020
val setup = Isabelle.Setup(isabelleHome = Path.of("/opt/Isabelle2020"), logic = "HOL")
implicit val isabelle: Isabelle = new Isabelle(setup)

// Load the Isabelle/HOL theory "Main" and create a context object
val ctxt = Context("Main")

// Create a term by parsing a string
val term = Term(ctxt, "x+0 = (y::nat)*1")

// A function to replace occurrences of X+1 by X (for all X)
def replace(term: Term): Term = term match {
  case App(App(Const("Groups.plus_class.plus", _), x), Const("Groups.zero_class.zero", _)) => replace(x)
  case Abs(name, typ, body) => Abs(name, typ, replace(body))
  case App(t1, t2) => App(replace(t1), replace(t2))
  case _ => term
}

// Replace x+0 by x in the term above
val term2 = replace(term)

// And pretty print the term
println("term2: " + term2.pretty(ctxt))
// ==> term2: x = y * 1

// Compile an ML function that can be executed directly in the Isabelle process
val simplify : MLFunction2[Context, Term, Term] =
  MLValue.compileFunction("fn (ctxt,t) => Thm.cterm_of ctxt t |> Simplifier.asm_full_rewrite ctxt " +
    "|> Thm.rhs_of |> Thm.term_of")

// Simplify term2
val term3 = simplify(ctxt,term2).retrieveNow

println("term3: " + term3.pretty(ctxt))
// ==> term3: x = y
```
The source code for this example can be found in [Example.scala](https://raw.githubusercontent.com/dominique-unruh/scala-isabelle/master/src/test/scala/de/unruh/isabelle/Example.scala).

For more information, see the API doc for classes `de.unruh.isabelle.control.Isabelle`
and `de.unruh.isabelle.mlvalue.MLValue` and `de.unruh.isabelle.mlvalue.Term`.
(Note: the Scaladoc is not hosted anywhere yet, so you have to look at the Scaladoc
comments in the source code.)
