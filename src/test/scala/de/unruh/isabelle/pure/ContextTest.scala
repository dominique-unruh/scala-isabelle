package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleTest.isabelle

import scala.concurrent.ExecutionContext.Implicits.global

class ContextTest extends org.scalatest.funsuite.AnyFunSuite {
  test("exception is E_Context") {
    assert(Context.exceptionName == "E_Context")
  }
}
