package de.unruh.isabelle.pure

import de.unruh.isabelle.control.IsabelleException
import de.unruh.isabelle.control.IsabelleTest.{isabelle => isa}
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global

class TheoryTest extends AnyFunSuite {
  test("import structure") {
    assertThrows[IsabelleException] {
      isa.executeMLCodeNow("HOLogic.boolT") }
    val thy = Theory("Main")
    thy.importMLStructure("HOLogic", "MyHOLogic")
    isa.executeMLCodeNow("MyHOLogic.boolT")
  }
}
