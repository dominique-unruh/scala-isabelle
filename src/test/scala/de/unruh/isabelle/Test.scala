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
import de.unruh.isabelle.control.IsabelleTest.setup

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

    val waitASec = MLValue.compileFunction0[Unit]("fn () => OS.Process.sleep (seconds 10.0)")
    val quick = MLValue.compileFunction0[Int]("K 1")

    println("Starting waitASec")
    val w = waitASec()
    println("Starting/retrieving quick")
    quick().retrieveNow
    println("Retrieving waitASec")
    w.retrieveNow
    println("Done")
  }
}
