package de.unruh.isabelle.pure

import org.scalatest.funsuite.AnyFunSuite

import de.unruh.isabelle.control.IsabelleTest.isabelle

class ThmTest extends AnyFunSuite {
  test("retrieve thm") {
    val ctxt = Context("Main")
    val thm = Thm(ctxt, "HOL.TrueI")
    thm.proposition match {
      case App(Const("HOL.Trueprop",_), Const("HOL.True", _)) =>
      case _ => fail("Proposition is: "+thm.proposition.pretty(ctxt))
    }
  }

  test("retrieve thm shortname") {
    val ctxt = Context("Main")
    val thm = Thm(ctxt, "TrueI")
    val thm2 = Thm(ctxt, "HOL.TrueI")
    assert(thm.pretty(ctxt) == thm2.pretty(ctxt))
  }
}
