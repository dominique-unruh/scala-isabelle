package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite


class ToplevelStateTest extends AnyFunSuite {
  test ("theory of") {
    val thy1 = Theory("Main")
    val state = ToplevelState(thy1)
    assert(state.mode == ToplevelState.Modes.Theory)
    assert(state.isEndTheory == false)
    val thy2 = state.theory
    thy2.force
  }

  test("context of") {
    val thy = Theory("Main")
    val state = ToplevelState(thy)
    val ctxt = state.context
    ctxt.force
  }

  test ("initial state") {
    val state = ToplevelState()
    assert(state.mode == ToplevelState.Modes.Toplevel)
    assert(state.isEndTheory == false)
  }
}
