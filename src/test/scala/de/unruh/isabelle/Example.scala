package de.unruh.isabelle

import _root_.java.nio.file.Path

import de.unruh.isabelle.mlvalue.{MLFunction2, MLValue}
import de.unruh.isabelle.pure.{Abs, App, Const, Term}
import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.pure.Context

// Implicits
import de.unruh.isabelle.mlvalue.Implicits._
import de.unruh.isabelle.pure.Implicits._
import scala.concurrent.ExecutionContext.Implicits.global

object Example {
  def main(args: Array[String]): Unit = {

    // Initialize the Isabelle process with session HOL.
    // The first command line argument must be the Isabelle installation directory
    val isabelleHome = args(0)
    // Differs from example in README: we skip building to make tests faster
    val setup = Isabelle.Setup(isabelleHome = Path.of(isabelleHome), logic = "HOL", build=false)
    implicit val isabelle: Isabelle = new Isabelle(setup)

    // Load the Isabelle/HOL theory "Main" and create a context object
    val ctxt = Context("Main")

    // Create a term by parsing a string
    val term = Term(ctxt, "x+0 = (y::nat)*1")

    // A function to replace occurrences of X+0 by X (for all X)
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

    // Destroy to save resources. (Not needed if the application ends here anyway.)
    isabelle.destroy()
  }
}
