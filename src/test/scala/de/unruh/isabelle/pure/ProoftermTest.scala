package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleException
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.Version
import de.unruh.isabelle.pure.Proofterm.PThm
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class ProoftermTest extends AnyFunSuite {

  test("old version") {
    if (!Version.from2020) {
      val exn = intercept[IsabelleException] { PThm(Thm(Context("Main"), "HOL.refl")).proof }
      assert(exn.message.startsWith("Proofterms are supported only for Isabelle >=2020, not "))
    }
  }

  test("get refl") {
    if (Version.from2020) {
      val ctxt = Context("Main")
      val prf = PThm(Thm(ctxt, "HOL.refl")).proof
      print(prf)
    }
  }
}
