package de.unruh.isabelle.pure

import java.nio.file.{Files, Path, Paths}

import de.unruh.isabelle.control.IsabelleException
import de.unruh.isabelle.control.IsabelleTest.setup
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
    println("Step 1") // TODO remove
    val num1 : MLValue[Int] = MLValue.compileValue("ImportMe.num")     // fails
    println("Step 2") // TODO remove
    assertThrows[IsabelleException] { num1.retrieveNow }
    println("Step 3") // TODO remove
    val importMe : String = thy.importMLStructureNow("ImportMe")     // import the structure Test into the ML toplevel
    println("Step 4") // TODO remove
    val num2 : MLValue[Int] = MLValue.compileValue(s"${importMe}.num") // access Test (under new name) in compiled ML code
    println("Step 5") // TODO remove
    assert(num2.retrieveNow == 123)                                              // ==> 123
    println("Step 6") // TODO remove
  }

  test("load theory outside heap") {
    Theory.registerSessionDirectoriesNow("HOL-Library" -> setup.isabelleHome.resolve("src/HOL/Library"))
    Theory("HOL-Library.BigO").force
  }

  test("load theory inside heap") {
    Theory("HOL.Set").force
  }

  test("load theory by path") {
    val thyPath = Paths.get("Empty.thy")
    assert(Files.exists(setup.workingDirectory.resolve(thyPath)))
    Theory(thyPath).force
  }

  test("load theory by path, nested") {
    val thyPath = Paths.get("Subdir/B.thy")
    assert(Files.exists(setup.workingDirectory.resolve(thyPath)))
    Theory(thyPath).force
  }

  test("registerSessionDirectories loaded session") {
    Theory.registerSessionDirectoriesNow("HOL" -> setup.isabelleHome.resolve("src/HOL"))
    Theory("HOL.Filter").force
  }

  test("registerSessionDirectories loaded session, wrong session dir") {
    val badHOL = Path.of("src/test/isabelle/Bad-HOL").toAbsolutePath
    assert(Files.isDirectory(badHOL))
    Theory.registerSessionDirectoriesNow("HOL" -> badHOL)
    val thy = Theory("HOL.Filter").force
    val ctxt = Context(thy)
    val thm = Thm(ctxt, "Filter.filter_eq_iff").force
    println(thm.pretty(ctxt))
  }

}
