package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class ToplevelStateTest extends AnyFunSuite {
  test ("theory of") {
    val thy1 = Theory("Main")
    val state = ToplevelState(thy1)
    val thy2 = state.theory
    thy2.force
  }

  test("context of") {
    val thy = Theory("Main")
    val state = ToplevelState(thy)
    val ctxt = state.context
    ctxt.force
  }
}
