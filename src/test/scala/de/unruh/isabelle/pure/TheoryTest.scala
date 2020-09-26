package de.unruh.isabelle.pure

import java.nio.file.{Files, Paths}

import de.unruh.isabelle.control.IsabelleException
import de.unruh.isabelle.control.IsabelleTest.isabelle
import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.ExecutionContext.Implicits.global
import scala.reflect.io.Path

class TheoryTest extends AnyFunSuite {
  test("import structure") {
    assertThrows[IsabelleException] {
      isabelle.executeMLCodeNow("HOLogic.boolT") }
    val thy = Theory("Main")
    thy.importMLStructure("HOLogic", "MyHOLogic")
    isabelle.executeMLCodeNow("MyHOLogic.boolT")
  }

  test("load theory outside heap") {
    Theory.registerSessionDirectoriesNow("HOL-Library" -> isabelle.setup.isabelleHome.resolve("src/HOL/Library"))
    Theory("HOL-Library.BigO").force
  }

  test("load theory inside heap") {
    Theory("HOL.Set").force
  }

  test("load theory by path") {
    val thyPath = Paths.get("src/test/isabelle/Control_Isabelle.thy")
    assert(Files.exists(thyPath))
    Theory(thyPath).force
  }

}
