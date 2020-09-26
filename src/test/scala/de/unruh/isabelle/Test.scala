package de.unruh.isabelle

import java.nio.file.{Files, Path}

import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLValue}
import de.unruh.isabelle.pure.{Abs, App, Const, Context, TFree, TVar, Term, Theory, Typ}
import org.scalatest.funsuite.AnyFunSuite

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import MLValue.{compileFunction, compileValue}

import scala.collection.JavaConverters.asScalaIteratorConverter

// Implicits
import scala.concurrent.ExecutionContext.Implicits.global
import de.unruh.isabelle.control.IsabelleTest.isabelle
import pure.Implicits._
import mlvalue.Implicits._

class Test extends AnyFunSuite {
  test("README example") {
    Example.main(Array(IsabelleTest.isabelleHome.toString))
  }


  test("temporary experiments") {
    MLValue.init()
    Theory.init()

    val theoryName: MLFunction[Theory, String] =
      MLValue.compileFunction("Context.theory_id #> Context.theory_id_long_name")
    def thyName(thy: Theory) = theoryName(thy).retrieveNow
    def printThyName(thy: Theory): Unit = println(thyName(thy))


//    val known = ListBuffer[(String,Path)]()
//
//    def addFromSession(session: String, path: Path): Unit =
//      for (file <- Files.list(path).iterator().asScala;
//           fileName = file.getFileName.toString;
//           if fileName.endsWith(".thy");
//           thyName = fileName.stripSuffix(".thy"))
//        known += session + "." + thyName -> path.resolve(file)
//
//    addFromSession("HOL-Library", isabelle.setup.isabelleHome.resolve("src/HOL/Library"))
//    addFromSession("Program-Conflict-Analysis", Path.of("/opt/afp-2019/thys/Program-Conflict-Analysis"))

//    Theory.registerTheory(known.toSeq: _*)

    Theory.registerTheoryPaths("HOL-Library" -> isabelle.setup.isabelleHome.resolve("src/HOL/Library"))

//    updateKnownTheories(known.toList).retrieveNow

    val thyMain = Theory("Main")
    printThyName(thyMain)

    val thyHolSet = Theory("HOL.Set")
    printThyName(thyHolSet)

    val thyAList = Theory("HOL-Library.AList")
    printThyName(thyAList)

    val thyBigO = Theory("HOL-Library.BigO")
    printThyName(thyBigO)
  }
}
