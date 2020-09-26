package de.unruh.isabelle.pure

import java.nio.file.{Files, Path, Paths}

import de.unruh.isabelle.control.IsabelleException
import de.unruh.isabelle.mlvalue.MLValue
import org.scalatest.funsuite.AnyFunSuite

// Implicits
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.control.IsabelleTest.isabelle
import de.unruh.isabelle.mlvalue.Implicits._

class TheoryTest extends AnyFunSuite {
  test("import structure") {
    assertThrows[IsabelleException] {
      isabelle.executeMLCodeNow("HOLogic.boolT") }
    val thy = Theory("Main")
    val struct = thy.importMLStructureNow("HOLogic")
    println(struct)
    isabelle.executeMLCodeNow(s"${struct}.boolT")
  }

  test("importMLStructureNow example") {
    val thy : Theory = Theory(Path.of("ImportMeThy.thy"))                         // load the theory TestThy
    val num1 : MLValue[Int] = MLValue.compileValue("ImportMe.num")     // fails
    assertThrows[IsabelleException] { num1.retrieveNow }
    val importMe : String = thy.importMLStructureNow("ImportMe")     // import the structure Test into the ML toplevel
    val num2 : MLValue[Int] = MLValue.compileValue(s"${importMe}.num") // access Test (under new name) in compiled ML code
    assert(num2.retrieveNow == 123)                                              // ==> 123
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
