package de.unruh.isabelle

import _root_.java.nio.file.{Files, Path}

import de.unruh.isabelle.control.IsabelleTest
import de.unruh.isabelle.mlvalue.{MLFunction, MLFunction2, MLFunction3, MLValue, Version}
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

  test("Java example") {
    JavaExample.main(Array(IsabelleTest.isabelleHome.toString))
  }

  test("temporary experiments") {
    MLValue.init()
    Theory.init()

    if (!Version.from2020) cancel("Only works for Isabelle2020+")

    val theoryName: MLFunction[Theory, String] =
      MLValue.compileFunction("Context.theory_id #> Context.theory_id_long_name")
    def thyName(thy: Theory) = theoryName(thy).retrieveNow
    def printThyName(thy: Theory): Unit = println(thyName(thy))

    Theory.registerSessionDirectoriesNow("HOL-Library" -> isabelle.setup.isabelleHome.resolve("src/HOL/Library"))

    println(MLValue.compileValue[Option[String]]("Resources.find_theory_file \"HOL-Library.AList\" |> Option.map Path.implode").retrieveNow)

//    isabelle.destroy()
//    Thread.sleep(3000)

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
