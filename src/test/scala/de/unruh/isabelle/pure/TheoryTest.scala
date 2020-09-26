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
    val struct = thy.importMLStructureNow("HOLogic")
    println(struct)
    isabelle.executeMLCodeNow(s"${struct}.boolT")
  }

  test("load theory outside heap") {
    Theory.registerSessionDirectoriesNow("HOL-Library" -> isabelle.setup.isabelleHome.resolve("src/HOL/Library"))
    Theory("HOL-Library.BigO").force
  }

  test("load theory inside heap") {
    Theory("HOL.Set").force
  }

  test("load theory by path") {
    val thyPath = Paths.get("Empty.thy")
    assert(Files.exists(isabelle.setup.workingDirectory.resolve(thyPath)))
    Theory(thyPath).force
  }

}
