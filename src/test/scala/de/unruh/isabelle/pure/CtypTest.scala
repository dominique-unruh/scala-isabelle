package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite

class CtypTest extends AnyFunSuite {
  val ctxt: Context = Context("Main")

  test ("Create Ctyp from term") {
    val term = Typ(ctxt, "nat => nat")
    val cterm = Ctyp(ctxt, term)
    assert(cterm == term)
  }

  test("Create Ctyp from string") {
    val term = Typ(ctxt, "string")
    val cterm = Ctyp(ctxt, "string")
    assert(cterm == term)
  }

  test("Create Ctyp from Ctyp, same context") {
    val cterm = Ctyp(ctxt, "bool")
    val cterm2 = Ctyp(ctxt, cterm)
    assert(cterm == cterm2)
  }

  test("Create Ctyp from Ctyp, supertheory") {
    val cterm = Ctyp(ctxt, "bool")
    val ctxt2 = Context("Complex_Main")
    val cterm2 = Ctyp(ctxt2, cterm)
    assert(cterm == cterm2)
  }

  test("Create Ctyp from Ctyp, unrelated theory") {
    val cterm = Ctyp(ctxt, "bool")
    val ctxt2 = Context("HOL.Complex")
    val cterm2 = Ctyp(ctxt2, cterm)
    assert(cterm == cterm2)
  }
}
