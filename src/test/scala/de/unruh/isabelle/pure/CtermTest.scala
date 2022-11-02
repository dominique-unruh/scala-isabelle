package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite

class CtermTest extends AnyFunSuite {
  val ctxt: Context = Context("Main")

  test ("Create Cterm from term") {
    val term = Term(ctxt, "1+1::nat")
    val cterm = Cterm(ctxt, term)
    assert(cterm == term)
  }

  test("Create Cterm from string") {
    val term = Term(ctxt, "x+y=z")
    val cterm = Cterm(ctxt, "x+y=z")
    assert(cterm == term)
  }

  test("Create Cterm from Cterm, same context") {
    val cterm = Cterm(ctxt, "undefined")
    val cterm2 = Cterm(ctxt, cterm)
    assert(cterm == cterm2)
  }

  test("Create Cterm from Cterm, supertheory") {
    val cterm = Cterm(ctxt, "undefined")
    val ctxt2 = Context("Complex_Main")
    val cterm2 = Cterm(ctxt2, cterm)
    assert(cterm == cterm2)
  }

  test("Create Cterm from Cterm, unrelated theory") {
    val cterm = Cterm(ctxt, "undefined")
    val ctxt2 = Context("HOL.Complex")
    val cterm2 = Cterm(ctxt2, cterm)
    assert(cterm == cterm2)
  }
}
