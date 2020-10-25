package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.pure.Proofterm.PThm
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class ProoftermTest extends AnyFunSuite {
  test("get refl") {
    val ctxt = Context("Main")
    val prf = PThm(Thm(ctxt, "HOL.refl")).proof
    print(prf)
  }
}
