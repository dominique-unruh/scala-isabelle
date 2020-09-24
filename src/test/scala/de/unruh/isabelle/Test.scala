package de.unruh.isabelle

import java.nio.file.Path

import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.mlvalue.{MLFunction2, MLValue}
import de.unruh.isabelle.pure.{Abs, App, Const, Context, TFree, TVar, Term, Typ}
import org.scalatest.funsuite.AnyFunSuite

// Implicits
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.control.IsabelleTest.isabelle
import pure.Implicits._

class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }


  test("temporary experiments") {
    // TODO: Implement schematic parsing in Typ and Term
    Context.init()
    val setSchematic = MLValue.compileFunction[Context, Context](
      "Proof_Context.set_mode Proof_Context.mode_schematic")
    val ctxt = setSchematic(Context("Main")).retrieveNow
    val t = Typ(ctxt, "?'a")
    t match {
      case TVar(name, index, sort) =>
        println(name)
        println(index)
        println(sort)
    }
  }
}
